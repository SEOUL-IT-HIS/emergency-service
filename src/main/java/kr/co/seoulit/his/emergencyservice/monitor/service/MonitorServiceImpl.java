package kr.co.seoulit.his.emergencyservice.monitor.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.monitor.dto.*;
import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import kr.co.seoulit.his.emergencyservice.monitor.repository.LosAlertRepository;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.patient.dto.PatientDto;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionMetricDto;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final LosAlertRepository losAlertRepository;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final DischargeProgress dischargeProgress;
    private final ResourceService resourceService;
    private final PatientClient patientClient;

    // 장기체류 기준(분). 기본 6시간 — getLongStayAlerts 기본값과 같음
    @Value("${app.monitor.los-threshold-minutes:360}")
    private int losThresholdMinutes;

    /** 취소된 접수는 재실 환자·장기체류 알림 대상이 아니다 */
    private List<ReceptionIntake> activeIntakes() {
        return receptionIntakeRepository.findAll().stream().filter(intake -> !intake.isCancelled()).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        CongestionMetricDto congestion = resourceService.getCongestion().getTotal();
        List<ReceptionIntake> intakes = activeIntakes();
        // 재실 = 퇴실 처리가 끝나지 않은 접수(환자 목록 '진료 중'과 같은 기준). 입원 병상 대기 환자도 재실이다.
        Set<String> done = doneReceptionIds(intakes);
        List<LosAlert> openAlerts = openAlertsOfPatientsInCare(done);

        DashboardDto dto = new DashboardDto();
        dto.setCurrentPatients(intakes.stream()
                .filter(intake -> !done.contains(intake.getId()))
                .count());
        dto.setOccupiedBeds(congestion.getOccupiedBeds());
        dto.setAvailableBeds(congestion.getAvailableBeds());
        dto.setCongestionRate(congestion.getCongestionRate());
        dto.setCongestionLevel(congestion.getCongestionLevel());
        dto.setOpenLosAlerts(openAlerts.size());
        dto.setRecentLosAlerts(toDtoList(openAlerts.stream().limit(20).toList()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LosAlertDto> getLongStayAlerts(Integer thresholdHours) {
        int minutes = thresholdHours != null ? thresholdHours * 60 : 360;
        Set<String> done = doneReceptionIds(activeIntakes());
        List<LosAlert> alerts = openAlertsOfPatientsInCare(done).stream()
                .filter(a -> a.getThresholdMinutes() == null || a.getThresholdMinutes() >= minutes)
                .toList();
        return toDtoList(alerts);
    }

    /**
     * 재실 환자(퇴실 처리가 끝나지 않은 접수 — 입원 병상 대기·전원 소견서 전 포함) 중 접수 후 기준시간을 넘긴 건에 LOS_ALERT 를 만든다.
     * 같은 기준시간 알림은 접수 건당 1번만 — 확인(acknowledge)한 뒤 다시 울리지 않는다.
     */
    @Override
    @Transactional
    public int detectLongStayPatients() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(losThresholdMinutes);
        List<ReceptionIntake> intakes = activeIntakes();
        Set<String> disposed = doneReceptionIds(intakes);
        // 이미 알린 접수는 한 번에 가져와 메모리에서 확인한다(접수마다 exists 조회 = N+1 방지)
        Set<String> alreadyAlerted = new HashSet<>(losAlertRepository.findReceptionIdsByThresholdMinutes(losThresholdMinutes));

        int created = 0;
        for (ReceptionIntake intake : intakes) {
            if (intake.getReceivedAt() == null || intake.getReceivedAt().isAfter(cutoff)
                    || disposed.contains(intake.getId())
                    || alreadyAlerted.contains(intake.getId())) {
                continue;
            }
            LosAlert alert = new LosAlert();
            alert.setReceptionId(intake.getId());
            alert.setThresholdMinutes(losThresholdMinutes);
            alert.setTriggeredAt(now);
            alert.setCreatedAt(now);
            alert.setUpdatedAt(now);
            losAlertRepository.save(alert);
            created++;
        }
        return created;
    }

    @Override
    @Transactional
    public LosAlertDto acknowledgeLongStayAlert(String alertId, LosAlertAcknowledgeRequestDto request) {
        if (!StringUtils.hasText(request.getAcknowledgedById())) {
            throw new IllegalArgumentException("acknowledgedById is required");
        }
        LosAlert alert = losAlertRepository.findById(alertId)
                .orElseThrow(() -> ResourceNotFoundException.of("losAlert", alertId));
        if (alert.getAcknowledgedAt() != null) {
            throw new ConflictException("long-stay alert already acknowledged: " + alertId);
        }
        alert.setAcknowledgedById(request.getAcknowledgedById());
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setUpdatedAt(LocalDateTime.now());
        return toDtoList(List.of(alert)).get(0);
    }

    /**
     * LosAlert 목록을 DTO로 변환하면서 환자명을 붙인다.
     * LosAlert 는 receptionId 만 가지고 있어서 ReceptionIntake 로 patientId 를 먼저 찾고,
     * patient-service 배치조회는 목록당 한 번만 호출한다(N+1 방지, CareServiceImpl.getPatients 와 동일 패턴).
     */
    private List<LosAlertDto> toDtoList(List<LosAlert> alerts) {
        if (alerts.isEmpty()) {
            return List.of();
        }

        List<String> receptionIds = alerts.stream().map(LosAlert::getReceptionId).distinct().toList();
        // patientId 가 없는 접수는 뺀다(toMap 은 null 값을 받지 못해 현황판 전체가 500 이 된다)
        Map<String, String> patientIdByReceptionId = receptionIntakeRepository.findAllById(receptionIds).stream()
                .filter(intake -> intake.getPatientId() != null)
                .collect(Collectors.toMap(ReceptionIntake::getId, ReceptionIntake::getPatientId, (a, b) -> a));

        List<String> patientIds = patientIdByReceptionId.values().stream()
                .filter(StringUtils::hasText)
                .filter(this::isValidPatientId)
                .distinct()
                .toList();
        // 환자서비스가 응답하지 않아도 현황판은 떠야 한다(이름만 비움, 환자 목록과 같은 처리)
        Map<String, String> patientNames;
        try {
            patientNames = patientClient.getPatients(patientIds).stream()
                    .collect(Collectors.toMap(PatientDto::getPatientId, PatientDto::getPatientName, (a, b) -> a));
        } catch (RuntimeException e) {
            log.warn("환자서비스 조회 실패 - 환자명 없이 장기체류 알림을 내려줍니다: {}", e.getMessage());
            patientNames = Collections.emptyMap(); // Map.of() 는 get(null) 에서 예외가 나므로 쓰지 않는다
        }
        final Map<String, String> names = patientNames;

        return alerts.stream().map(alert -> {
            LosAlertDto dto = new LosAlertDto();
            dto.setId(alert.getId());
            dto.setReceptionId(alert.getReceptionId());
            dto.setPatientName(names.get(patientIdByReceptionId.get(alert.getReceptionId())));
            dto.setThresholdMinutes(alert.getThresholdMinutes());
            dto.setTriggeredAt(alert.getTriggeredAt());
            dto.setAcknowledgedById(alert.getAcknowledgedById());
            dto.setAcknowledgedAt(alert.getAcknowledgedAt());
            return dto;
        }).toList();
    }

    // PAT은 UUID 형식이 아닌 값이 하나라도 섞이면 요청 전체를 400으로 거부하므로,
    // 형식이 잘못된 값은 호출 전에 걸러내야 나머지 정상 건까지 같이 실패하지 않는다 (CareServiceImpl과 동일 이유).
    private boolean isValidPatientId(String patientId) {
        try {
            UUID.fromString(patientId);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("PAT 배치조회 대상에서 제외 - patientId가 UUID 형식이 아님: {}", patientId);
            return false;
        }
    }

    private Set<String> doneReceptionIds(List<ReceptionIntake> intakes) {
        return dischargeProgress.doneReceptionIds(intakes.stream().map(ReceptionIntake::getId).toList());
    }

    /** 미확인 알림 중 아직 재실 중인 환자 것만 — 퇴실 처리가 끝난 환자의 알림은 현황판에서 뺀다 */
    private List<LosAlert> openAlertsOfPatientsInCare(Set<String> done) {
        return losAlertRepository.findByAcknowledgedAtIsNull().stream()
                .filter(a -> !done.contains(a.getReceptionId()))
                .toList();
    }
}
