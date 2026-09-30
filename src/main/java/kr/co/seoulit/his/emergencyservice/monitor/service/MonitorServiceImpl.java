package kr.co.seoulit.his.emergencyservice.monitor.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.monitor.dto.*;
import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import kr.co.seoulit.his.emergencyservice.monitor.repository.LosAlertRepository;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionMetricDto;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final LosAlertRepository losAlertRepository;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final DispositionRepository dispositionRepository;
    private final ResourceService resourceService;

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
        dto.setRecentLosAlerts(openAlerts.stream().limit(20).map(this::toDto).toList());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LosAlertDto> getLongStayAlerts(Integer thresholdHours) {
        int minutes = thresholdHours != null ? thresholdHours * 60 : 360;
        return losAlertRepository.findByAcknowledgedAtIsNull().stream()
                .filter(a -> a.getThresholdMinutes() == null || a.getThresholdMinutes() >= minutes)
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalHospitalDto> getExternalHospitals() {
        // NEDIS 연동 전 stub — 외부 규격 확정 후 교체
        List<ExternalHospitalDto> list = new ArrayList<>();
        ExternalHospitalDto sample = new ExternalHospitalDto();
        sample.setHospitalCode("11100000");
        sample.setHospitalName("NEDIS stub hospital");
        sample.setRegion("SEOUL");
        sample.setAvailableBeds(0);
        list.add(sample);
        return list;
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
        return toDto(alert);
    }

    private LosAlertDto toDto(LosAlert entity) {
        LosAlertDto dto = new LosAlertDto();
        dto.setId(entity.getId());
        dto.setReceptionId(entity.getReceptionId());
        dto.setThresholdMinutes(entity.getThresholdMinutes());
        dto.setTriggeredAt(entity.getTriggeredAt());
        dto.setAcknowledgedById(entity.getAcknowledgedById());
        dto.setAcknowledgedAt(entity.getAcknowledgedAt());
        return dto;
    }
}
