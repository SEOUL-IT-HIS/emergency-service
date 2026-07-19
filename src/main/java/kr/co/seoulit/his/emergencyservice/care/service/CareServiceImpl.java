package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.entity.*;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.resource.entity.BedAssignment;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
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
