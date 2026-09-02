const fs = require('fs');
const path = require('path');
const BASE = 'C:/his/emergency-service/src/main/java/kr/co/seoulit/his/emergencyservice';
const PKG = 'kr.co.seoulit.his.emergencyservice';

function write(rel, content) {
  const full = path.join(BASE, rel);
  fs.mkdirSync(path.dirname(full), { recursive: true });
  fs.writeFileSync(full, content, 'utf8');
  console.log('W', rel);
}

const AUDIT = `
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
`;

// ===================== CHANNEL =====================
write('channel/entity/ConsultRequest.java', `package ${PKG}.channel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "CONSULT_REQUEST")
@Getter
@Setter
public class ConsultRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONSULT_REQUEST_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "TARGET_DEPT_CODE", length = 20)
    private String targetDeptCode;

    @Column(name = "CONSULT_STATUS_CODE", length = 20)
    private String consultStatusCode;

    @Column(name = "REASON", length = 4000)
    private String reason;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    @Column(name = "REPLIED_AT")
    private LocalDateTime repliedAt;
${AUDIT}
}
`);

write('channel/entity/OncallRequest.java', `package ${PKG}.channel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "ONCALL_REQUEST")
@Getter
@Setter
public class OncallRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ONCALL_REQUEST_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "TARGET_ROLE_CODE", length = 20)
    private String targetRoleCode;

    @Column(name = "CALLED_BY_ID", length = 36)
    private String calledById;

    @Column(name = "CALLED_AT")
    private LocalDateTime calledAt;

    @Column(name = "RESPONDED_AT")
    private LocalDateTime respondedAt;
${AUDIT}
}
`);

write('channel/entity/SurgeryRequest.java', `package ${PKG}.channel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "SURGERY_REQUEST")
@Getter
@Setter
public class SurgeryRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SURGERY_REQUEST_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "PROCEDURE_CODE", length = 30)
    private String procedureCode;

    @Column(name = "REQUEST_STATUS_CODE", length = 20)
    private String requestStatusCode;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;
${AUDIT}
}
`);

['ConsultRequest', 'OncallRequest', 'SurgeryRequest'].forEach(name => {
  write(`channel/repository/${name}Repository.java`, `package ${PKG}.channel.repository;

import ${PKG}.channel.entity.${name};
import org.springframework.data.jpa.repository.JpaRepository;

public interface ${name}Repository extends JpaRepository<${name}, Long> {
}
`);
});

write('channel/dto/ConsultCreateRequestDto.java', `package ${PKG}.channel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultCreateRequestDto {
    private String encounterId;
    private String specialty;
    private String reason;
    private Long orderId;
    private String channelRef;
}
`);

write('channel/dto/ConsultRequestDto.java', `package ${PKG}.channel.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ConsultRequestDto {
    private Long id;
    private String receptionNo;
    private String targetDeptCode;
    private String consultStatusCode;
    private String reason;
    private Long orderId;
    private LocalDateTime requestedAt;
}
`);

write('channel/dto/OncallCreateRequestDto.java', `package ${PKG}.channel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OncallCreateRequestDto {
    private String encounterId;
    private String targetRole;
    private String calledById;
}
`);

write('channel/dto/OncallRequestDto.java', `package ${PKG}.channel.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class OncallRequestDto {
    private Long id;
    private String receptionNo;
    private String targetRoleCode;
    private String calledById;
    private LocalDateTime calledAt;
}
`);

write('channel/service/ChannelService.java', `package ${PKG}.channel.service;

import ${PKG}.channel.dto.*;

public interface ChannelService {
    ConsultRequestDto createConsultation(ConsultCreateRequestDto request);
    OncallRequestDto createOnCallPage(OncallCreateRequestDto request);
}
`);

write('channel/service/ChannelServiceImpl.java', `package ${PKG}.channel.service;

import ${PKG}.channel.dto.*;
import ${PKG}.channel.entity.ConsultRequest;
import ${PKG}.channel.entity.OncallRequest;
import ${PKG}.channel.repository.ConsultRequestRepository;
import ${PKG}.channel.repository.OncallRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChannelServiceImpl implements ChannelService {

    private final ConsultRequestRepository consultRequestRepository;
    private final OncallRequestRepository oncallRequestRepository;

    @Override
    @Transactional
    public ConsultRequestDto createConsultation(ConsultCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getSpecialty())) {
            throw new IllegalArgumentException("encounterId and specialty are required");
        }
        ConsultRequest entity = new ConsultRequest();
        entity.setReceptionNo(request.getEncounterId());
        entity.setTargetDeptCode(request.getSpecialty());
        entity.setReason(request.getReason());
        entity.setOrderId(request.getOrderId());
        entity.setConsultStatusCode("REQUESTED");
        entity.setRequestedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        ConsultRequest saved = consultRequestRepository.save(entity);

        ConsultRequestDto dto = new ConsultRequestDto();
        dto.setId(saved.getId());
        dto.setReceptionNo(saved.getReceptionNo());
        dto.setTargetDeptCode(saved.getTargetDeptCode());
        dto.setConsultStatusCode(saved.getConsultStatusCode());
        dto.setReason(saved.getReason());
        dto.setOrderId(saved.getOrderId());
        dto.setRequestedAt(saved.getRequestedAt());
        return dto;
    }

    @Override
    @Transactional
    public OncallRequestDto createOnCallPage(OncallCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getTargetRole())) {
            throw new IllegalArgumentException("encounterId and targetRole are required");
        }
        OncallRequest entity = new OncallRequest();
        entity.setReceptionNo(request.getEncounterId());
        entity.setTargetRoleCode(request.getTargetRole());
        entity.setCalledById(request.getCalledById());
        entity.setCalledAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        OncallRequest saved = oncallRequestRepository.save(entity);

        OncallRequestDto dto = new OncallRequestDto();
        dto.setId(saved.getId());
        dto.setReceptionNo(saved.getReceptionNo());
        dto.setTargetRoleCode(saved.getTargetRoleCode());
        dto.setCalledById(saved.getCalledById());
        dto.setCalledAt(saved.getCalledAt());
        return dto;
    }
}
`);

write('channel/controller/ChannelController.java', `package ${PKG}.channel.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.channel.dto.*;
import ${PKG}.channel.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping("/consultations")
    public ApiResponse<ConsultRequestDto> createConsultation(@RequestBody ConsultCreateRequestDto request) {
        return ApiResponse.success(channelService.createConsultation(request));
    }

    @PostMapping("/on-call-pages")
    public ApiResponse<OncallRequestDto> createOnCallPage(@RequestBody OncallCreateRequestDto request) {
        return ApiResponse.success(channelService.createOnCallPage(request));
    }
}
`);

// ===================== MONITOR =====================
write('monitor/entity/LosAlert.java', `package ${PKG}.monitor.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "LOS_ALERT")
@Getter
@Setter
public class LosAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LOS_ALERT_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "THRESHOLD_MINUTES")
    private Integer thresholdMinutes;

    @Column(name = "TRIGGERED_AT")
    private LocalDateTime triggeredAt;

    @Column(name = "ACKNOWLEDGED_BY_ID", length = 36)
    private String acknowledgedById;

    @Column(name = "ACKNOWLEDGED_AT")
    private LocalDateTime acknowledgedAt;
${AUDIT}
}
`);

write('monitor/repository/LosAlertRepository.java', `package ${PKG}.monitor.repository;

import ${PKG}.monitor.entity.LosAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LosAlertRepository extends JpaRepository<LosAlert, Long> {
    List<LosAlert> findByAcknowledgedAtIsNull();
}
`);

write('monitor/dto/DashboardDto.java', `package ${PKG}.monitor.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class DashboardDto {
    private long totalPatients;
    private long occupiedBeds;
    private long emptyBeds;
    private long openLosAlerts;
    private List<LosAlertDto> recentLosAlerts;
}
`);

write('monitor/dto/LosAlertDto.java', `package ${PKG}.monitor.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class LosAlertDto {
    private Long id;
    private String receptionNo;
    private Integer thresholdMinutes;
    private LocalDateTime triggeredAt;
}
`);

write('monitor/dto/ExternalHospitalDto.java', `package ${PKG}.monitor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalHospitalDto {
    private String hospitalCode;
    private String hospitalName;
    private String region;
    private Integer availableBeds;
}
`);

write('monitor/service/MonitorService.java', `package ${PKG}.monitor.service;

import ${PKG}.monitor.dto.*;
import java.util.List;

public interface MonitorService {
    DashboardDto getDashboard();
    List<LosAlertDto> getLongStayAlerts(Integer thresholdHours);
    List<ExternalHospitalDto> getExternalHospitals();
}
`);

write('monitor/service/MonitorServiceImpl.java', `package ${PKG}.monitor.service;

import ${PKG}.monitor.dto.*;
import ${PKG}.monitor.entity.LosAlert;
import ${PKG}.monitor.repository.LosAlertRepository;
import ${PKG}.resource.dto.CongestionDto;
import ${PKG}.resource.service.ResourceService;
import ${PKG}.triage.repository.TriageAssessmentRepository;
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
                .map(a -> a.getReceptionNo()).distinct().count());
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
        // NEDIS ?∞Îèô ??stub ???∏Î? Í∑úÍ≤© ?ïÏ†ï ??ÍµêÏ≤¥
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
        dto.setReceptionNo(entity.getReceptionNo());
        dto.setThresholdMinutes(entity.getThresholdMinutes());
        dto.setTriggeredAt(entity.getTriggeredAt());
        return dto;
    }
}
`);

write('monitor/controller/MonitorController.java', `package ${PKG}.monitor.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.monitor.dto.*;
import ${PKG}.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> getDashboard() {
        return ApiResponse.success(monitorService.getDashboard());
    }

    @GetMapping("/long-stay-alerts")
    public ApiResponse<List<LosAlertDto>> getLongStayAlerts(
            @RequestParam(required = false) Integer thresholdHours) {
        return ApiResponse.success(monitorService.getLongStayAlerts(thresholdHours));
    }

    @GetMapping("/external-hospitals")
    public ApiResponse<List<ExternalHospitalDto>> getExternalHospitals() {
        return ApiResponse.success(monitorService.getExternalHospitals());
    }
}
`);

console.log('channel+monitor done');
