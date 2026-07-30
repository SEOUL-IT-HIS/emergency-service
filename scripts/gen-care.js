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

// ===================== CARE =====================
write('care/entity/ClinicalNote.java', `package ${PKG}.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "CLINICAL_NOTE")
@Getter
@Setter
public class ClinicalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLINICAL_NOTE_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "RECORDED_BY_ID", length = 36)
    private String recordedById;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "RECORDED_AT")
    private LocalDateTime recordedAt;

    @Column(name = "SIGNED_AT")
    private LocalDateTime signedAt;
${AUDIT}
}
`);

write('care/entity/TreatmentRecord.java', `package ${PKG}.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "TREATMENT_RECORD")
@Getter
@Setter
public class TreatmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TREATMENT_RECORD_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "TREATMENT_TYPE_CODE", length = 20)
    private String treatmentTypeCode;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "PERFORMED_BY_ID", length = 36)
    private String performedById;

    @Column(name = "PERFORMED_AT")
    private LocalDateTime performedAt;
${AUDIT}
}
`);

write('care/entity/MedicationAdministration.java', `package ${PKG}.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "MEDICATION_ADMINISTRATION")
@Getter
@Setter
public class MedicationAdministration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MEDICATION_ADMINISTRATION_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "ORDER_ITEM_ID")
    private Long orderItemId;

    @Column(name = "DRUG_CODE", length = 20)
    private String drugCode;

    @Column(name = "DOSE", length = 30)
    private String dose;

    @Column(name = "ROUTE_CODE", length = 10)
    private String routeCode;

    @Column(name = "ADMINISTERED_BY_ID", length = 36)
    private String administeredById;

    @Column(name = "ADMINISTERED_AT")
    private LocalDateTime administeredAt;
${AUDIT}
}
`);

write('care/entity/CprEvent.java', `package ${PKG}.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "EMERGENCY", name = "CPR_EVENT")
@Getter
@Setter
public class CprEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CPR_EVENT_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt;

    @Column(name = "OUTCOME_CODE", length = 20)
    private String outcomeCode;

    @OneToMany(mappedBy = "cprEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CprTimeline> timelines = new ArrayList<>();
${AUDIT}
}
`);

write('care/entity/CprTimeline.java', `package ${PKG}.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "CPR_TIMELINE")
@Getter
@Setter
public class CprTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CPR_TIMELINE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CPR_EVENT_ID", nullable = false)
    private CprEvent cprEvent;

    @Column(name = "EVENT_AT")
    private LocalDateTime eventAt;

    @Column(name = "EVENT_TYPE_CODE", length = 20)
    private String eventTypeCode;

    @Column(name = "DETAIL", length = 4000)
    private String detail;

    @Column(name = "RECORDED_BY_ID", length = 36)
    private String recordedById;
${AUDIT}
}
`);

['ClinicalNote', 'TreatmentRecord', 'MedicationAdministration', 'CprEvent'].forEach(name => {
  write(`care/repository/${name}Repository.java`, `package ${PKG}.care.repository;

import ${PKG}.care.entity.${name};
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ${name}Repository extends JpaRepository<${name}, Long> {
    List<${name}> findByReceptionNo(String receptionNo);
}
`);
});

write('care/dto/EmergencyPatientDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmergencyPatientDto {
    private String receptionNo;
    private String ktasLevelCode;
    private String careStatusCode;
    private String bedNo;
    private String zoneCode;
    private LocalDateTime lastAssessedAt;
}
`);

write('care/dto/ClinicalNoteCreateRequestDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicalNoteCreateRequestDto {
    private String encounterId;
    private String content;
    private String recordedById;
}
`);

write('care/dto/ClinicalNoteDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ClinicalNoteDto {
    private Long id;
    private String receptionNo;
    private String content;
    private String recordedById;
    private LocalDateTime recordedAt;
}
`);

write('care/dto/TreatmentCreateRequestDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreatmentCreateRequestDto {
    private String encounterId;
    private Long orderId;
    private String treatmentCode;
    private String description;
    private String performedById;
}
`);

write('care/dto/TreatmentRecordDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TreatmentRecordDto {
    private Long id;
    private String receptionNo;
    private Long orderId;
    private String treatmentTypeCode;
    private String description;
    private String performedById;
    private LocalDateTime performedAt;
}
`);

write('care/dto/MarCreateRequestDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class MarCreateRequestDto {
    private String encounterId;
    private Long orderId;
    private Long orderItemId;
    private String drugCode;
    private String dose;
    private String routeCode;
    private String administeredById;
    private LocalDateTime administeredAt;
}
`);

write('care/dto/MarDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class MarDto {
    private Long id;
    private String receptionNo;
    private Long orderId;
    private Long orderItemId;
    private String drugCode;
    private String dose;
    private String routeCode;
    private String administeredById;
    private LocalDateTime administeredAt;
}
`);

write('care/dto/CprTimelineCreateRequestDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CprTimelineCreateRequestDto {
    private String encounterId;
    private String outcomeCode;
    private List<CprEventItemDto> events;

    @Getter
    @Setter
    public static class CprEventItemDto {
        private LocalDateTime eventAt;
        private String eventTypeCode;
        private String detail;
        private String recordedById;
    }
}
`);

write('care/dto/CprEventDto.java', `package ${PKG}.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CprEventDto {
    private Long id;
    private String receptionNo;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String outcomeCode;
    private List<CprTimelineItemDto> timelines;

    @Getter
    @Setter
    public static class CprTimelineItemDto {
        private Long id;
        private LocalDateTime eventAt;
        private String eventTypeCode;
        private String detail;
        private String recordedById;
    }
}
`);

write('care/mapper/CareMapstructMapper.java', `package ${PKG}.care.mapper;

import ${PKG}.care.dto.*;
import ${PKG}.care.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CareMapstructMapper {
    ClinicalNoteDto toNoteDto(ClinicalNote entity);

    @Mapping(target = "treatmentTypeCode", source = "treatmentTypeCode")
    TreatmentRecordDto toTreatmentDto(TreatmentRecord entity);

    MarDto toMarDto(MedicationAdministration entity);
}
`);

write('care/service/CareService.java', `package ${PKG}.care.service;

import ${PKG}.care.dto.*;
import java.util.List;

public interface CareService {
    List<EmergencyPatientDto> getPatients(String date, String status);
    ClinicalNoteDto createRecord(ClinicalNoteCreateRequestDto request);
    TreatmentRecordDto createTreatment(TreatmentCreateRequestDto request);
    MarDto createMar(MarCreateRequestDto request);
    CprEventDto createCprTimeline(CprTimelineCreateRequestDto request);
}
`);

write('care/service/CareServiceImpl.java', `package ${PKG}.care.service;

import ${PKG}.care.dto.*;
import ${PKG}.care.entity.*;
import ${PKG}.care.mapper.CareMapstructMapper;
import ${PKG}.care.repository.*;
import ${PKG}.resource.entity.BedAssignment;
import ${PKG}.resource.repository.BedAssignmentRepository;
import ${PKG}.triage.entity.TriageAssessment;
import ${PKG}.triage.repository.TriageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CareServiceImpl implements CareService {

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final TreatmentRecordRepository treatmentRecordRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final CprEventRepository cprEventRepository;
    private final TriageAssessmentRepository triageAssessmentRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final CareMapstructMapper careMapper;

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyPatientDto> getPatients(String date, String status) {
        List<TriageAssessment> assessments = triageAssessmentRepository.findAll();
        Map<String, TriageAssessment> latestByReception = new LinkedHashMap<>();
        for (TriageAssessment assessment : assessments) {
            if (!StringUtils.hasText(assessment.getReceptionNo())) {
                continue;
            }
            if (StringUtils.hasText(date)) {
                LocalDate filterDate = LocalDate.parse(date);
                if (assessment.getAssessedAt() == null
                        || !assessment.getAssessedAt().toLocalDate().equals(filterDate)) {
                    continue;
                }
            }
            TriageAssessment existing = latestByReception.get(assessment.getReceptionNo());
            if (existing == null
                    || (assessment.getAssessedAt() != null
                    && (existing.getAssessedAt() == null
                    || assessment.getAssessedAt().isAfter(existing.getAssessedAt())))) {
                latestByReception.put(assessment.getReceptionNo(), assessment);
            }
        }

        return latestByReception.values().stream().map(assessment -> {
            EmergencyPatientDto dto = new EmergencyPatientDto();
            dto.setReceptionNo(assessment.getReceptionNo());
            dto.setKtasLevelCode(assessment.getKtasLevelCode());
            dto.setLastAssessedAt(assessment.getAssessedAt());
            dto.setCareStatusCode("IN_CARE");
            List<BedAssignment> beds =
                    bedAssignmentRepository.findByReceptionNoAndReleasedAtIsNull(assessment.getReceptionNo());
            if (!beds.isEmpty() && beds.get(0).getBed() != null) {
                dto.setBedNo(beds.get(0).getBed().getBedNo());
                dto.setZoneCode(beds.get(0).getBed().getZoneCode());
            }
            return dto;
        }).filter(dto -> !StringUtils.hasText(status) || status.equals(dto.getCareStatusCode()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClinicalNoteDto createRecord(ClinicalNoteCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("encounterId and content are required");
        }
        ClinicalNote entity = new ClinicalNote();
        entity.setReceptionNo(request.getEncounterId());
        entity.setContent(request.getContent());
        entity.setRecordedById(request.getRecordedById());
        entity.setRecordedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return careMapper.toNoteDto(clinicalNoteRepository.save(entity));
    }

    @Override
    @Transactional
    public TreatmentRecordDto createTreatment(TreatmentCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getTreatmentCode())) {
            throw new IllegalArgumentException("encounterId and treatmentCode are required");
        }
        TreatmentRecord entity = new TreatmentRecord();
        entity.setReceptionNo(request.getEncounterId());
        entity.setOrderId(request.getOrderId());
        entity.setTreatmentTypeCode(request.getTreatmentCode());
        entity.setDescription(request.getDescription());
        entity.setPerformedById(request.getPerformedById());
        entity.setPerformedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return careMapper.toTreatmentDto(treatmentRecordRepository.save(entity));
    }

    @Override
    @Transactional
    public MarDto createMar(MarCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || request.getOrderId() == null
                || request.getAdministeredAt() == null || !StringUtils.hasText(request.getDose())) {
            throw new IllegalArgumentException("encounterId, orderId, administeredAt, dose are required");
        }
        MedicationAdministration entity = new MedicationAdministration();
        entity.setReceptionNo(request.getEncounterId());
        entity.setOrderId(request.getOrderId());
        entity.setOrderItemId(request.getOrderItemId());
        entity.setDrugCode(request.getDrugCode());
        entity.setDose(request.getDose());
        entity.setRouteCode(request.getRouteCode());
        entity.setAdministeredById(request.getAdministeredById());
        entity.setAdministeredAt(request.getAdministeredAt());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return careMapper.toMarDto(medicationAdministrationRepository.save(entity));
    }

    @Override
    @Transactional
    public CprEventDto createCprTimeline(CprTimelineCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId())
                || request.getEvents() == null || request.getEvents().isEmpty()) {
            throw new IllegalArgumentException("encounterId and events[] are required");
        }
        CprEvent event = new CprEvent();
        event.setReceptionNo(request.getEncounterId());
        event.setStartedAt(LocalDateTime.now());
        event.setOutcomeCode(request.getOutcomeCode());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        for (CprTimelineCreateRequestDto.CprEventItemDto item : request.getEvents()) {
            CprTimeline timeline = new CprTimeline();
            timeline.setCprEvent(event);
            timeline.setEventAt(item.getEventAt() != null ? item.getEventAt() : LocalDateTime.now());
            timeline.setEventTypeCode(item.getEventTypeCode());
            timeline.setDetail(item.getDetail());
            timeline.setRecordedById(item.getRecordedById());
            timeline.setCreatedAt(LocalDateTime.now());
            timeline.setUpdatedAt(LocalDateTime.now());
            event.getTimelines().add(timeline);
        }

        CprEvent saved = cprEventRepository.save(event);
        CprEventDto dto = new CprEventDto();
        dto.setId(saved.getId());
        dto.setReceptionNo(saved.getReceptionNo());
        dto.setStartedAt(saved.getStartedAt());
        dto.setEndedAt(saved.getEndedAt());
        dto.setOutcomeCode(saved.getOutcomeCode());
        dto.setTimelines(saved.getTimelines().stream().map(t -> {
            CprEventDto.CprTimelineItemDto item = new CprEventDto.CprTimelineItemDto();
            item.setId(t.getId());
            item.setEventAt(t.getEventAt());
            item.setEventTypeCode(t.getEventTypeCode());
            item.setDetail(t.getDetail());
            item.setRecordedById(t.getRecordedById());
            return item;
        }).collect(Collectors.toList()));
        return dto;
    }
}
`);

write('care/controller/CareController.java', `package ${PKG}.care.controller;

import ${PKG}.common.ApiResponse;
import ${PKG}.care.dto.*;
import ${PKG}.care.service.CareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency/care")
@RequiredArgsConstructor
public class CareController {

    private final CareService careService;

    @GetMapping("/patients")
    public ApiResponse<List<EmergencyPatientDto>> getPatients(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(careService.getPatients(date, status));
    }

    @PostMapping("/records")
    public ApiResponse<ClinicalNoteDto> createRecord(@RequestBody ClinicalNoteCreateRequestDto request) {
        return ApiResponse.success(careService.createRecord(request));
    }

    @PostMapping("/treatments")
    public ApiResponse<TreatmentRecordDto> createTreatment(@RequestBody TreatmentCreateRequestDto request) {
        return ApiResponse.success(careService.createTreatment(request));
    }

    @PostMapping("/medication-administrations")
    public ApiResponse<MarDto> createMar(@RequestBody MarCreateRequestDto request) {
        return ApiResponse.success(careService.createMar(request));
    }

    @PostMapping("/cpr-timelines")
    public ApiResponse<CprEventDto> createCprTimeline(@RequestBody CprTimelineCreateRequestDto request) {
        return ApiResponse.success(careService.createCprTimeline(request));
    }
}
`);

console.log('care done');
