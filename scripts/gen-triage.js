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

function entityHeader(className, table, idCol) {
  return `package ${PKG}.${arguments[3] || 'x'}.entity;
`;
}

// ---- helpers for audit fields ----
const AUDIT = `
    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
`;

// ===================== TRIAGE =====================
write('triage/entity/EmsReferral.java', `package ${PKG}.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "EMS_REFERRAL")
@Getter
@Setter
public class EmsReferral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMS_REFERRAL_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "EMS_AGENCY_NAME", length = 100)
    private String emsAgencyName;

    @Column(name = "VITALS_ON_SCENE", length = 4000)
    private String vitalsOnScene;

    @Column(name = "PREHOSPITAL_TREATMENT", length = 4000)
    private String prehospitalTreatment;

    @Column(name = "TRANSMITTED_AT")
    private LocalDateTime transmittedAt;
${AUDIT}
}
`);

write('triage/entity/TriageAssessment.java', `package ${PKG}.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "TRIAGE_ASSESSMENT")
@Getter
@Setter
public class TriageAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRIAGE_ASSESSMENT_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "KTAS_LEVEL_CODE", length = 10)
    private String ktasLevelCode;

    @Column(name = "ASSESSMENT_TYPE_CODE", length = 20)
    private String assessmentTypeCode;

    @Column(name = "ASSESSED_BY_ID", length = 36)
    private String assessedById;

    @Column(name = "ASSESSED_AT")
    private LocalDateTime assessedAt;

    @Column(name = "REASON", length = 4000)
    private String reason;
${AUDIT}
}
`);

write('triage/entity/EwsRecord.java', `package ${PKG}.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "EWS_RECORD")
@Getter
@Setter
public class EwsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EWS_RECORD_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "SYSTOLIC_BP")
    private Integer systolicBp;

    @Column(name = "HEART_RATE")
    private Integer heartRate;

    @Column(name = "RESP_RATE")
    private Integer respRate;

    @Column(name = "TEMPERATURE", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "SPO2")
    private Integer spo2;

    @Column(name = "GCS")
    private Integer gcs;

    @Column(name = "EWS_SCORE", precision = 5, scale = 2)
    private BigDecimal ewsScore;

    @Column(name = "MEASURED_BY_ID", length = 36)
    private String measuredById;

    @Column(name = "MEASURED_AT")
    private LocalDateTime measuredAt;
${AUDIT}
}
`);

write('triage/entity/IsolationAssessment.java', `package ${PKG}.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "ISOLATION_ASSESSMENT")
@Getter
@Setter
public class IsolationAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ISOLATION_ASSESSMENT_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ISOLATION_TYPE_CODE", length = 20)
    private String isolationTypeCode;

    @Column(name = "REQUIRED_YN", length = 1)
    private String requiredYn;

    @Column(name = "DECIDED_BY_ID", length = 36)
    private String decidedById;

    @Column(name = "DECIDED_AT")
    private LocalDateTime decidedAt;

    @Column(name = "RELEASED_AT")
    private LocalDateTime releasedAt;
${AUDIT}
}
`);

write('triage/entity/RiskScreening.java', `package ${PKG}.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "RISK_SCREENING")
@Getter
@Setter
public class RiskScreening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RISK_SCREENING_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "SCREENING_TYPE_CODE", length = 20)
    private String screeningTypeCode;

    @Column(name = "SCORE", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "RESULT_CODE", length = 20)
    private String resultCode;

    @Column(name = "SCREENED_BY_ID", length = 36)
    private String screenedById;

    @Column(name = "SCREENED_AT")
    private LocalDateTime screenedAt;
${AUDIT}
}
`);

['EmsReferral', 'TriageAssessment', 'EwsRecord', 'IsolationAssessment', 'RiskScreening'].forEach(name => {
  write(`triage/repository/${name}Repository.java`, `package ${PKG}.triage.repository;

import ${PKG}.triage.entity.${name};
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ${name}Repository extends JpaRepository<${name}, Long> {
    List<${name}> findByReceptionNo(String receptionNo);
}
`);
});

write('triage/dto/EmsReferralDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmsReferralDto {
    private Long id;
    private String receptionNo;
    private String emsAgencyName;
    private String vitalsOnScene;
    private String prehospitalTreatment;
    private LocalDateTime transmittedAt;
}
`);

write('triage/dto/KtasCreateRequestDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KtasCreateRequestDto {
    private String patientId;
    private String encounterId;
    private String ktasScore;
    private String assessmentTypeCode;
    private String assessedById;
    private String reason;
}
`);

write('triage/dto/KtasUpdateRequestDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KtasUpdateRequestDto {
    private String ktasScore;
    private String assessedById;
    private String reason;
}
`);

write('triage/dto/TriageAssessmentDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TriageAssessmentDto {
    private Long id;
    private String receptionNo;
    private String ktasLevelCode;
    private String assessmentTypeCode;
    private String assessedById;
    private LocalDateTime assessedAt;
    private String reason;
}
`);

write('triage/dto/VitalAssessmentCreateRequestDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class VitalAssessmentCreateRequestDto {
    private String encounterId;
    private String measuredById;
    private List<VitalItemDto> vitals;

    @Getter
    @Setter
    public static class VitalItemDto {
        private Integer systolicBp;
        private Integer heartRate;
        private Integer respRate;
        private BigDecimal temperature;
        private Integer spo2;
        private Integer gcs;
        private BigDecimal ewsScore;
    }
}
`);

write('triage/dto/EwsRecordDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EwsRecordDto {
    private Long id;
    private String receptionNo;
    private Integer systolicBp;
    private Integer heartRate;
    private Integer respRate;
    private BigDecimal temperature;
    private Integer spo2;
    private Integer gcs;
    private BigDecimal ewsScore;
    private String measuredById;
    private LocalDateTime measuredAt;
}
`);

write('triage/dto/IsolationCreateRequestDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IsolationCreateRequestDto {
    private String patientId;
    private String encounterId;
    private String isolationTypeCode;
    private String requiredYn;
    private String decidedById;
}
`);

write('triage/dto/IsolationAssessmentDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class IsolationAssessmentDto {
    private Long id;
    private String receptionNo;
    private String isolationTypeCode;
    private String requiredYn;
    private String decidedById;
    private LocalDateTime decidedAt;
    private LocalDateTime releasedAt;
}
`);

write('triage/dto/RiskScreeningCreateRequestDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class RiskScreeningCreateRequestDto {
    private String encounterId;
    private String screenType;
    private BigDecimal score;
    private String resultCode;
    private String screenedById;
}
`);

write('triage/dto/RiskScreeningDto.java', `package ${PKG}.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class RiskScreeningDto {
    private Long id;
    private String receptionNo;
    private String screeningTypeCode;
    private BigDecimal score;
    private String resultCode;
    private String screenedById;
    private LocalDateTime screenedAt;
}
`);

write('triage/mapper/TriageMapstructMapper.java', `package ${PKG}.triage.mapper;

import ${PKG}.triage.dto.*;
import ${PKG}.triage.entity.*;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TriageMapstructMapper {
    EmsReferralDto toEmsDto(EmsReferral entity);
    List<EmsReferralDto> toEmsDtoList(List<EmsReferral> list);
    TriageAssessmentDto toKtasDto(TriageAssessment entity);
    EwsRecordDto toEwsDto(EwsRecord entity);
    List<EwsRecordDto> toEwsDtoList(List<EwsRecord> list);
    IsolationAssessmentDto toIsolationDto(IsolationAssessment entity);
    RiskScreeningDto toRiskDto(RiskScreening entity);
}
`);

write('triage/service/TriageService.java', `package ${PKG}.triage.service;

import ${PKG}.triage.dto.*;
import java.util.List;

public interface TriageService {
    List<EmsReferralDto> getEmsInfo(String receptionNo);
    TriageAssessmentDto createKtas(KtasCreateRequestDto request);
    TriageAssessmentDto updateKtas(Long id, KtasUpdateRequestDto request);
    List<EwsRecordDto> createVitalAssessments(VitalAssessmentCreateRequestDto request);
    IsolationAssessmentDto createIsolation(IsolationCreateRequestDto request);
    RiskScreeningDto createRiskScreening(RiskScreeningCreateRequestDto request);
}
`);

write('triage/service/TriageServiceImpl.java', `package ${PKG}.triage.service;

import ${PKG}.triage.dto.*;
import ${PKG}.triage.entity.*;
import ${PKG}.triage.mapper.TriageMapstructMapper;
import ${PKG}.triage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private final EmsReferralRepository emsReferralRepository;
    private final TriageAssessmentRepository triageAssessmentRepository;
    private final EwsRecordRepository ewsRecordRepository;
    private final IsolationAssessmentRepository isolationAssessmentRepository;
    private final RiskScreeningRepository riskScreeningRepository;
    private final TriageMapstructMapper triageMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmsReferralDto> getEmsInfo(String receptionNo) {
        if (StringUtils.hasText(receptionNo)) {
            return triageMapper.toEmsDtoList(emsReferralRepository.findByReceptionNo(receptionNo));
        }
        return triageMapper.toEmsDtoList(emsReferralRepository.findAll());
    }

    @Override
    @Transactional
    public TriageAssessmentDto createKtas(KtasCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getKtasScore())) {
            throw new IllegalArgumentException("encounterId and ktasScore are required");
        }
        TriageAssessment entity = new TriageAssessment();
        entity.setReceptionNo(request.getEncounterId());
        entity.setKtasLevelCode(request.getKtasScore());
        entity.setAssessmentTypeCode(
                StringUtils.hasText(request.getAssessmentTypeCode()) ? request.getAssessmentTypeCode() : "INITIAL");
        entity.setAssessedById(request.getAssessedById());
        entity.setAssessedAt(LocalDateTime.now());
        entity.setReason(request.getReason());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toKtasDto(triageAssessmentRepository.save(entity));
    }

    @Override
    @Transactional
    public TriageAssessmentDto updateKtas(Long id, KtasUpdateRequestDto request) {
        TriageAssessment entity = triageAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ktas not found: " + id));
        if (StringUtils.hasText(request.getKtasScore())) {
            entity.setKtasLevelCode(request.getKtasScore());
        }
        entity.setAssessmentTypeCode("REASSESS");
        if (request.getAssessedById() != null) {
            entity.setAssessedById(request.getAssessedById());
        }
        if (request.getReason() != null) {
            entity.setReason(request.getReason());
        }
        entity.setAssessedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toKtasDto(entity);
    }

    @Override
    @Transactional
    public List<EwsRecordDto> createVitalAssessments(VitalAssessmentCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || request.getVitals() == null || request.getVitals().isEmpty()) {
            throw new IllegalArgumentException("encounterId and vitals[] are required");
        }
        List<EwsRecord> saved = new ArrayList<>();
        for (VitalAssessmentCreateRequestDto.VitalItemDto vital : request.getVitals()) {
            EwsRecord entity = new EwsRecord();
            entity.setReceptionNo(request.getEncounterId());
            entity.setSystolicBp(vital.getSystolicBp());
            entity.setHeartRate(vital.getHeartRate());
            entity.setRespRate(vital.getRespRate());
            entity.setTemperature(vital.getTemperature());
            entity.setSpo2(vital.getSpo2());
            entity.setGcs(vital.getGcs());
            entity.setEwsScore(vital.getEwsScore());
            entity.setMeasuredById(request.getMeasuredById());
            entity.setMeasuredAt(LocalDateTime.now());
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            saved.add(ewsRecordRepository.save(entity));
        }
        return triageMapper.toEwsDtoList(saved);
    }

    @Override
    @Transactional
    public IsolationAssessmentDto createIsolation(IsolationCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) && !StringUtils.hasText(request.getPatientId())) {
            throw new IllegalArgumentException("patientId or encounterId is required");
        }
        IsolationAssessment entity = new IsolationAssessment();
        entity.setReceptionNo(StringUtils.hasText(request.getEncounterId())
                ? request.getEncounterId() : request.getPatientId());
        entity.setIsolationTypeCode(request.getIsolationTypeCode());
        entity.setRequiredYn(StringUtils.hasText(request.getRequiredYn()) ? request.getRequiredYn() : "Y");
        entity.setDecidedById(request.getDecidedById());
        entity.setDecidedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toIsolationDto(isolationAssessmentRepository.save(entity));
    }

    @Override
    @Transactional
    public RiskScreeningDto createRiskScreening(RiskScreeningCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getScreenType())) {
            throw new IllegalArgumentException("encounterId and screenType are required");
        }
        RiskScreening entity = new RiskScreening();
        entity.setReceptionNo(request.getEncounterId());
        entity.setScreeningTypeCode(request.getScreenType());
        entity.setScore(request.getScore());
        entity.setResultCode(request.getResultCode());
        entity.setScreenedById(request.getScreenedById());
        entity.setScreenedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toRiskDto(riskScreeningRepository.save(entity));
    }
}
`);

write('triage/controller/TriageController.java', `package ${PKG}.triage.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.triage.dto.*;
import ${PKG}.triage.service.TriageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @GetMapping("/ems-info")
    public ApiResponse<List<EmsReferralDto>> getEmsInfo(
            @RequestParam(required = false) String receptionNo) {
        return ApiResponse.success(triageService.getEmsInfo(receptionNo));
    }

    @PostMapping("/ktas")
    public ApiResponse<TriageAssessmentDto> createKtas(@RequestBody KtasCreateRequestDto request) {
        return ApiResponse.success(triageService.createKtas(request));
    }

    @PutMapping("/ktas/{id}")
    public ApiResponse<TriageAssessmentDto> updateKtas(
            @PathVariable Long id,
            @RequestBody KtasUpdateRequestDto request) {
        return ApiResponse.success(triageService.updateKtas(id, request));
    }

    @PostMapping("/vital-assessments")
    public ApiResponse<List<EwsRecordDto>> createVitalAssessments(
            @RequestBody VitalAssessmentCreateRequestDto request) {
        return ApiResponse.success(triageService.createVitalAssessments(request));
    }

    @PostMapping("/infection-isolations")
    public ApiResponse<IsolationAssessmentDto> createIsolation(
            @RequestBody IsolationCreateRequestDto request) {
        return ApiResponse.success(triageService.createIsolation(request));
    }

    @PostMapping("/risk-screenings")
    public ApiResponse<RiskScreeningDto> createRiskScreening(
            @RequestBody RiskScreeningCreateRequestDto request) {
        return ApiResponse.success(triageService.createRiskScreening(request));
    }
}
`);

console.log('triage done');
