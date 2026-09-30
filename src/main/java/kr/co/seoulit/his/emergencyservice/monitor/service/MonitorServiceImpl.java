package kr.co.seoulit.his.emergencyservice.monitor.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
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
    private final DispositionRepository dispositionRepository;
    private final ResourceService resourceService;
    private final PatientClient patientClient;

    // 장기체류 기준(분). 기본 6시간 — getLongStayAlerts 기본값과 같음
    @Value("${app.monitor.los-threshold-minutes:360}")
    private int losThresholdMinutes;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        CongestionMetricDto congestion = resourceService.getCongestion().getTotal();
        List<LosAlert> openAlerts = losAlertRepository.findByAcknowledgedAtIsNull();
        Set<String> disposed = new HashSet<>(dispositionRepository.findDistinctReceptionIds());

        DashboardDto dto = new DashboardDto();
        dto.setCurrentPatients(receptionIntakeRepository.findAll().stream()
                .filter(intake -> !disposed.contains(intake.getId()))
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
        List<LosAlert> alerts = losAlertRepository.findByAcknowledgedAtIsNull().stream()
                .filter(a -> a.getThresholdMinutes() == null || a.getThresholdMinutes() >= minutes)
                .toList();
        return toDtoList(alerts);
    }

    /**
     * 재실 환자(퇴실 결정 없는 접수) 중 접수 후 기준시간을 넘긴 건에 LOS_ALERT 를 만든다.
     * 같은 기준시간 알림은 접수 건당 1번만 — 확인(acknowledge)한 뒤 다시 울리지 않는다.
     */
    @Override
    @Transactional
    public int detectLongStayPatients() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(losThresholdMinutes);
        Set<String> disposed = new HashSet<>(dispositionRepository.findDistinctReceptionIds());

        int created = 0;
        for (ReceptionIntake intake : receptionIntakeRepository.findAll()) {
            if (intake.getReceivedAt() == null || intake.getReceivedAt().isAfter(cutoff)
                    || disposed.contains(intake.getId())
                    || losAlertRepository.existsByReceptionIdAndThresholdMinutes(intake.getId(), losThresholdMinutes)) {
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
        Map<String, String> patientIdByReceptionId = receptionIntakeRepository.findAllById(receptionIds).stream()
                .collect(Collectors.toMap(ReceptionIntake::getId, ReceptionIntake::getPatientId, (a, b) -> a));

        List<String> patientIds = patientIdByReceptionId.values().stream()
                .filter(StringUtils::hasText)
                .filter(this::isValidPatientId)
                .distinct()
                .toList();
        Map<String, String> patientNames = patientClient.getPatients(patientIds).stream()
                .collect(Collectors.toMap(PatientDto::getPatientId, PatientDto::getPatientName));

        return alerts.stream().map(alert -> {
            LosAlertDto dto = new LosAlertDto();
            dto.setId(alert.getId());
            dto.setReceptionId(alert.getReceptionId());
            dto.setPatientName(patientNames.get(patientIdByReceptionId.get(alert.getReceptionId())));
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
}
