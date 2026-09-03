package kr.co.seoulit.his.emergencyservice.monitor.service;

import kr.co.seoulit.his.emergencyservice.monitor.dto.*;
import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import kr.co.seoulit.his.emergencyservice.monitor.repository.LosAlertRepository;
import kr.co.seoulit.his.emergencyservice.resource.dto.CongestionDto;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final LosAlertRepository losAlertRepository;
    private final TriageAssessmentRepository triageAssessmentRepository;
    private final ResourceService resourceService;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboard() {
        CongestionDto congestion = resourceService.getCongestion();
        List<LosAlert> openAlerts = losAlertRepository.findByAcknowledgedAtIsNull();

        DashboardDto dto = new DashboardDto();
        dto.setTotalPatients(triageAssessmentRepository.findAll().stream()
                .map(a -> a.getReceptionId()).distinct().count());
        dto.setOccupiedBeds(congestion.getOccupiedBeds());
        dto.setEmptyBeds(congestion.getEmptyBeds());
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

    private LosAlertDto toDto(LosAlert entity) {
        LosAlertDto dto = new LosAlertDto();
        dto.setId(entity.getId());
        dto.setReceptionId(entity.getReceptionId());
        dto.setThresholdMinutes(entity.getThresholdMinutes());
        dto.setTriggeredAt(entity.getTriggeredAt());
        return dto;
    }
}
