package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.entity.*;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.patient.dto.PatientDto;
import kr.co.seoulit.his.emergencyservice.resource.entity.BedAssignment;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareServiceImpl implements CareService {

    // RCP의 실제 공통코드 그룹(admin Common Codes 화면 "내원형태코드"). admin 캐시가 비어있을 때만
    // 아래 폴백값을 쓴다 - 2026-09-10 기준 실제 값(01 예약/02 당일방문/03 응급내원/04 전원환자).
    private static final String ARRIVAL_PATH_GROUP_CODE = "VISIT_FORM_CD";
    private static final Set<String> ARRIVAL_PATH_FALLBACK = Set.of("01", "02", "03", "04");

    private final ClinicalNoteRepository clinicalNoteRepository;
    private final TreatmentRecordRepository treatmentRecordRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final CprEventRepository cprEventRepository;
    private final TriageAssessmentRepository triageAssessmentRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final CommonCodeCache commonCodeCache;
    private final CommonCodeResolver codeResolver;
    private final CareMapstructMapper careMapper;
    private final PatientClient patientClient;

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyPatientDto> getPatients(String date, String status) {
        // 순회 기준 = ReceptionIntake(접수). 접수만 되어있으면 KTAS 전이라도 목록에 뜬다.
        List<ReceptionIntake> intakes = receptionIntakeRepository.findAll();

        // 환자명은 더 이상 접수 데이터에 저장하지 않고, PAT 배치조회로 채운다.
        // N번 개별 호출하지 않도록 patientId를 모아서 한 번만 호출한다.
        // PAT은 UUID 형식이 아닌 값이 하나라도 섞이면 요청 전체를 400으로 거부하므로,
        // 형식이 잘못된 값은 호출 전에 걸러내야 나머지 정상 건까지 같이 실패하지 않는다.
        List<String> patientIds = intakes.stream()
                .map(ReceptionIntake::getPatientId)
                .filter(StringUtils::hasText)
                .filter(this::isValidPatientId)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> patientNames = patientClient.getPatients(patientIds).stream()
                .collect(Collectors.toMap(PatientDto::getPatientId, PatientDto::getPatientName));

        return intakes.stream()
                .filter(intake -> {
                    if (!StringUtils.hasText(date)) {
                        return true;
                    }
                    LocalDate filterDate = LocalDate.parse(date);
                    return intake.getReceivedAt() != null
                            && intake.getReceivedAt().toLocalDate().equals(filterDate);
                })
                .map(intake -> {
                    EmergencyPatientDto dto = new EmergencyPatientDto();
                    dto.setReceptionId(intake.getId());
                    dto.setPatientName(patientNames.get(intake.getPatientId()));
                    dto.setReceivedAt(intake.getReceivedAt());
                    dto.setCareStatusCode("IN_CARE");

                    List<TriageAssessment> history =
                            triageAssessmentRepository.findByReceptionIdOrderByAssessedAtAsc(intake.getId());
                    if (!history.isEmpty()) {
                        TriageAssessment latest = history.get(history.size() - 1);
                        dto.setKtasLevelCode(latest.getKtasLevelCode());
                        dto.setLastAssessedAt(latest.getAssessedAt());
                    }

                    List<BedAssignment> beds =
                            bedAssignmentRepository.findByReceptionIdAndReleasedAtIsNull(intake.getId());
                    if (!beds.isEmpty() && beds.get(0).getBed() != null) {
                        dto.setBedNo(beds.get(0).getBed().getBedNo());
                        dto.setZoneCode(beds.get(0).getBed().getZoneCode());
                    }
                    return dto;
                })
                .filter(dto -> !StringUtils.hasText(status) || status.equals(dto.getCareStatusCode()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicalNoteDto> getRecords(String receptionId){
        if (!StringUtils.hasText(receptionId)){
            throw new IllegalArgumentException("receptionId is required");
        }

        List<ClinicalNote> notes = clinicalNoteRepository.findByReceptionIdOrderByRecordedAtAsc(receptionId);
        return notes.stream().map( note -> {
            ClinicalNoteDto dto = new ClinicalNoteDto();
            dto.setId(note.getId());
            dto.setReceptionId(note.getReceptionId());
            dto.setNoteTypeCode(note.getNoteTypeCode());
            dto.setContent(note.getContent());
            dto.setRecordedById(note.getRecordedById());
            dto.setRecordedAt(note.getRecordedAt());
            dto.setSignedAt(note.getSignedAt());
            return dto;
                }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClinicalNoteDto createRecord(ClinicalNoteCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getContent())
                || !StringUtils.hasText(request.getRecordedById())
                || !StringUtils.hasText(request.getNoteTypeCode())) {
            throw new IllegalArgumentException("encounterId, noteTypeCode, content, recordedById are required");
        }
        // 진료기록 종류: admin ER_NOTE_TYPE_CD(초진/재평가/처치/퇴실요약), 그룹이 없으면 폴백
        codeResolver.require("noteTypeCode", request.getNoteTypeCode(),
                codeResolver.valueSet(EmgCodes.NOTE_TYPE_GROUP, EmgCodes.NOTE_TYPE_FALLBACK));
        ClinicalNote entity = new ClinicalNote();
        entity.setReceptionId(request.getEncounterId());
        entity.setNoteTypeCode(request.getNoteTypeCode());
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
        // orderId 필수 검증: CLAUDE.md 12장 "투여/처치 기록(UC-CARE-03/04)에는 orderId 필수 검증 포함" 규정
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getOrderId())
                || !StringUtils.hasText(request.getTreatmentCode())
                || !StringUtils.hasText(request.getPerformedById())) {
            throw new IllegalArgumentException(
                    "encounterId, orderId, treatmentCode, performedById are required");
        }
        codeResolver.require("treatmentCode", request.getTreatmentCode(),
                codeResolver.valueSet(EmgCodes.TREATMENT_TYPE_GROUP, EmgCodes.TREATMENT_TYPE_FALLBACK));
        TreatmentRecord entity = new TreatmentRecord();
        entity.setReceptionId(request.getEncounterId());
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
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getOrderId())
                || request.getAdministeredAt() == null || !StringUtils.hasText(request.getDose())
                || !StringUtils.hasText(request.getDrugCode()) || !StringUtils.hasText(request.getRouteCode())
                || !StringUtils.hasText(request.getAdministeredById())) {
            throw new IllegalArgumentException(
                    "encounterId, orderId, administeredAt, dose, drugCode, routeCode, administeredById are required");
        }
        // 투여경로는 admin 기존 그룹 ADMIN_ROUTE_CD(01 PO, 02 IV ...) — 그룹이 없으면 폴백
        codeResolver.require("routeCode", request.getRouteCode(),
                codeResolver.valueSet(EmgCodes.ADMIN_ROUTE_GROUP, EmgCodes.ADMIN_ROUTE_FALLBACK));
        MedicationAdministration entity = new MedicationAdministration();
        entity.setReceptionId(request.getEncounterId());
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
        if (StringUtils.hasText(request.getOutcomeCode())) {
            codeResolver.require("outcomeCode", request.getOutcomeCode(),
                    codeResolver.valueSet(EmgCodes.CPR_OUTCOME_GROUP, EmgCodes.CPR_OUTCOME_FALLBACK));
        }
        Set<String> validEventTypes =
                codeResolver.valueSet(EmgCodes.CPR_EVENT_TYPE_GROUP, EmgCodes.CPR_EVENT_TYPE_FALLBACK);
        CprEvent event = new CprEvent();
        event.setReceptionId(request.getEncounterId());
        event.setStartedAt(LocalDateTime.now());
        event.setOutcomeCode(request.getOutcomeCode());
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        for (CprTimelineCreateRequestDto.CprEventItemDto item : request.getEvents()) {
            if (!StringUtils.hasText(item.getEventTypeCode()) || !StringUtils.hasText(item.getRecordedById())) {
                throw new IllegalArgumentException("each event requires eventTypeCode and recordedById");
            }
            codeResolver.require("eventTypeCode", item.getEventTypeCode(), validEventTypes);
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

        return toCprDto(cprEventRepository.save(event));
    }

    private CprEventDto toCprDto(CprEvent saved) {
        CprEventDto dto = new CprEventDto();
        dto.setId(saved.getId());
        dto.setReceptionId(saved.getReceptionId());
        dto.setStartedAt(saved.getStartedAt());
        dto.setEndedAt(saved.getEndedAt());
        dto.setOutcomeCode(saved.getOutcomeCode());
        dto.setTimelines(saved.getTimelines().stream()
                .sorted(java.util.Comparator.comparing(CprTimeline::getEventAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(tl -> {
                    CprEventDto.CprTimelineItemDto item = new CprEventDto.CprTimelineItemDto();
                    item.setId(tl.getId());
                    item.setEventAt(tl.getEventAt());
                    item.setEventTypeCode(tl.getEventTypeCode());
                    item.setDetail(tl.getDetail());
                    item.setRecordedById(tl.getRecordedById());
                    return item;
                }).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentRecordDto> getTreatments(String receptionId) {
        requireReceptionId(receptionId);
        return treatmentRecordRepository.findByReceptionIdOrderByPerformedAtAsc(receptionId).stream()
                .map(careMapper::toTreatmentDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MarDto> getMars(String receptionId) {
        requireReceptionId(receptionId);
        return medicationAdministrationRepository.findByReceptionIdOrderByAdministeredAtAsc(receptionId).stream()
                .map(careMapper::toMarDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CprEventDto> getCprEvents(String receptionId) {
        requireReceptionId(receptionId);
        return cprEventRepository.findByReceptionIdOrderByStartedAtDesc(receptionId).stream()
                .map(this::toCprDto).collect(Collectors.toList());
    }

    private void requireReceptionId(String receptionId) {
        if (!StringUtils.hasText(receptionId)) {
            throw new IllegalArgumentException("receptionId is required");
        }
    }

    @Override
    @Transactional
    public ReceptionIntakeDto createReceptionIntake(ReceptionIntakeCreateRequestDto request) {
        if (!StringUtils.hasText(request.getReceptionId())
                || !StringUtils.hasText(request.getPatientId())
                || !StringUtils.hasText(request.getArrivalPath())
                || request.getReceivedAt() == null) {
            throw new IllegalArgumentException(
                    "receptionId, patientId, arrivalPath, receivedAt are required");
        }
        Set<String> validArrivalPaths = validArrivalPaths();
        if (!validArrivalPaths.contains(request.getArrivalPath())) {
            throw new IllegalArgumentException(
                    "arrivalPath must be one of " + String.join(", ", validArrivalPaths));
        }

        ReceptionIntake intake = receptionIntakeRepository.findById(request.getReceptionId())
                .orElseGet(ReceptionIntake::new);
        boolean isNew = intake.getId() == null;

        intake.setId(request.getReceptionId());
        intake.setPatientId(request.getPatientId());
        intake.setArrivalPathCode(request.getArrivalPath());
        intake.setReceivedAt(request.getReceivedAt());
        intake.setMemo(request.getMemo());
        intake.setChiefComplaintRaw(request.getChiefComplaintRaw());
        intake.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            intake.setCreatedAt(LocalDateTime.now());
        }

        ReceptionIntake saved = receptionIntakeRepository.save(intake);

        ReceptionIntakeDto dto = new ReceptionIntakeDto();
        dto.setReceptionId(saved.getId());
        dto.setPatientId(saved.getPatientId());
        dto.setArrivalPath(saved.getArrivalPathCode());
        dto.setReceivedAt(saved.getReceivedAt());
        dto.setMemo(saved.getMemo());
        dto.setChiefComplaintRaw(saved.getChiefComplaintRaw());
        return dto;
    }

    /**
     * admin 공통코드 "내원형태코드"(VISIT_FORM_CD)를 조회한다. 서버 기동 시 admin이 안 떠있었거나
     * 아직 캐싱이 안 됐으면 빈 리스트가 오는데, 그럴 때 전부 막아버리면 RCP 연동이 통째로 끊기므로
     * 마지막으로 확인된 실제 값(ARRIVAL_PATH_FALLBACK)으로 대신 검증한다.
     */
    /** PAT 배치조회는 UUID 형식이 아닌 값이 하나라도 있으면 요청 전체를 400으로 거부한다. */
    private boolean isValidPatientId(String patientId) {
        try {
            UUID.fromString(patientId);
            return true;
        } catch (IllegalArgumentException e) {
            log.warn("PAT 배치조회 대상에서 제외 - patientId가 UUID 형식이 아님: {}", patientId);
            return false;
        }
    }

    private Set<String> validArrivalPaths() {
        List<AdminCommonCodeItemDto> codes = commonCodeCache.get(ARRIVAL_PATH_GROUP_CODE);
        if (codes.isEmpty()) {
            return ARRIVAL_PATH_FALLBACK;
        }
        return codes.stream()
                .filter(code -> !"N".equals(code.getUseYn()))
                .map(AdminCommonCodeItemDto::getCodeValue)
                .collect(Collectors.toSet());
    }
}
