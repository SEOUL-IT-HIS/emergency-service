package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.entity.*;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.util.InChunks;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
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
    private final DischargeProgress dischargeProgress;

    /** 목록 상태(저장하는 코드가 아니라 화면 필터용 계산값): 진료 중 / 퇴실 처리 완료 */
    public static final String CARE_STATUS_IN_CARE = "IN_CARE";
    public static final String CARE_STATUS_DONE = "DONE";
    /** 접수에서 취소한 접수(CANCELLED_AT 이 있는 접수) */
    public static final String CARE_STATUS_CANCELLED = "CANCELLED";

    @Override
    @Transactional(readOnly = true)
    public List<EmergencyPatientDto> getPatients(String date, String status) {
        // 순회 기준 = ReceptionIntake(접수). 접수만 되어있으면 KTAS 전이라도 목록에 뜬다.
        // 날짜·상태 조건을 먼저 걸러서, 아래 조회들이 화면에 나갈 접수만 대상으로 하게 한다.
        LocalDate filterDate = StringUtils.hasText(date) ? LocalDate.parse(date) : null;
        // 목록은 접수 순서(먼저 접수한 환자 먼저)로 내린다. findAll()은 순서를 보장하지 않고, 접수ID는 UUID라 순서와 무관하다.
        List<ReceptionIntake> dated = receptionIntakeRepository.findAll().stream()
                .filter(intake -> filterDate == null || (intake.getReceivedAt() != null
                        && intake.getReceivedAt().toLocalDate().equals(filterDate)))
                .sorted(Comparator.comparing(ReceptionIntake::getReceivedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ReceptionIntake::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        // 퇴실 처리 완료 기준은 DischargeProgress 하나로 통일(현황판·장기체류 알림과 같은 기준)
        Set<String> doneReceptionIds = dischargeProgress.doneReceptionIds(
                dated.stream().map(ReceptionIntake::getId).toList());
        List<ReceptionIntake> intakes = dated.stream()
                .filter(intake -> !StringUtils.hasText(status) || status.equals(careStatus(intake, doneReceptionIds)))
                .toList();
        List<String> receptionIds = intakes.stream().map(ReceptionIntake::getId).toList();

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

        // 환자서비스가 응답하지 않아도 목록은 떠야 한다(이름만 비움). 예전엔 여기서 예외가 나 목록 전체가 500 이었다.
        Map<String, String> patientNames;
        try {
            patientNames = patientClient.getPatients(patientIds).stream()
                    .collect(Collectors.toMap(PatientDto::getPatientId, PatientDto::getPatientName, (a, b) -> a));
        } catch (RuntimeException e) {
            log.warn("환자서비스 조회 실패 - 환자명 없이 목록을 내려줍니다: {}", e.getMessage());
            patientNames = Collections.emptyMap(); // Map.of() 는 get(null)(patientId 없는 접수)에서 예외가 나므로 쓰지 않는다
        }
        final Map<String, String> names = patientNames;

        // KTAS 이력·현재 병상은 접수마다 따로 조회하지 않고(N+1) 한 번에 가져와 접수ID로 묶는다.
        // 예전엔 환자 N명이면 쿼리가 2N번 이상(병상은 LAZY 라 배정마다 한 번 더) 나갔다.
        Map<String, List<TriageAssessment>> triageByReception = InChunks.query(receptionIds,
                        triageAssessmentRepository::findByReceptionIdInOrderByAssessedAtAsc).stream()
                .collect(Collectors.groupingBy(TriageAssessment::getReceptionId));
        Map<String, BedAssignment> bedByReception = InChunks.query(receptionIds,
                        bedAssignmentRepository::findActiveWithBedByReceptionIdIn).stream()
                .collect(Collectors.toMap(BedAssignment::getReceptionId, assignment -> assignment, (a, b) -> a));

        return intakes.stream()
                .map(intake -> {
                    EmergencyPatientDto dto = new EmergencyPatientDto();
                    dto.setReceptionId(intake.getId());
                    dto.setPatientName(names.get(intake.getPatientId()));
                    dto.setReceivedAt(intake.getReceivedAt());
                    dto.setCareStatusCode(careStatus(intake, doneReceptionIds));
                    dto.setMemo(intake.getMemo());
                    dto.setChiefComplaintRaw(intake.getChiefComplaintRaw());

                    // 오래된 순으로 가져왔으니 마지막이 최신 평가
                    List<TriageAssessment> history = triageByReception.getOrDefault(intake.getId(), List.of());
                    if (!history.isEmpty()) {
                        TriageAssessment latest = history.get(history.size() - 1);
                        dto.setKtasLevelCode(latest.getKtasLevelCode());
                        dto.setLastAssessedAt(latest.getAssessedAt());
                    }

                    BedAssignment assignment = bedByReception.get(intake.getId());
                    if (assignment != null && assignment.getBed() != null) {
                        dto.setBedNo(assignment.getBed().getBedNo());
                        dto.setZoneCode(assignment.getBed().getZoneCode());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private static String careStatus(ReceptionIntake intake, Set<String> doneReceptionIds) {
        if (intake.isCancelled()) {
            return CARE_STATUS_CANCELLED;
        }
        return doneReceptionIds.contains(intake.getId()) ? CARE_STATUS_DONE : CARE_STATUS_IN_CARE;
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
        dischargeProgress.requireNotCancelled(request.getEncounterId());
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
        dischargeProgress.requireNotCancelled(request.getEncounterId());
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
        dischargeProgress.requireNotCancelled(request.getEncounterId());
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
        dischargeProgress.requireNotCancelled(request.getEncounterId());
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
    @Transactional(readOnly = true)
    public List<ActiveReceptionDto> getActiveReceptions(String patientId, Integer sinceHours) {
        if (!StringUtils.hasText(patientId)) {
            throw new IllegalArgumentException("patientId is required");
        }
        int windowHours = sinceHours == null ? DEFAULT_ACTIVE_WINDOW_HOURS : sinceHours;
        if (windowHours <= 0) {
            throw new IllegalArgumentException("sinceHours must be greater than 0");
        }
        List<ActiveReceptionDto> result = activeReceptionsOf(patientId, windowHours).stream().map(intake -> {
            ActiveReceptionDto dto = new ActiveReceptionDto();
            dto.setReceptionId(intake.getId());
            dto.setPatientId(intake.getPatientId());
            dto.setReceivedAt(intake.getReceivedAt());
            return dto;
        }).toList();
        // 접수 서비스가 중복 접수 경고를 위해 호출했는지 확인하는 용도
        log.info("진행 중인 응급 접수 조회 - patientId={}, 최근 {}시간, 결과 {}건", patientId, windowHours, result.size());
        return result;
    }

    /**
     * 같은 환자의 퇴실 처리 전(DischargeProgress 기준) 접수를 접수 시각 오름차순으로.
     * 접수 후 windowHours 시간이 지난 건은 퇴실 누락·테스트 데이터로 보고 뺀다(접수 시각을 모르면 포함).
     */
    private List<ReceptionIntake> activeReceptionsOf(String patientId, int windowHours) {
        LocalDateTime since = LocalDateTime.now().minusHours(windowHours);
        List<ReceptionIntake> intakes = receptionIntakeRepository.findByPatientId(patientId).stream()
                .filter(intake -> !intake.isCancelled())
                .filter(intake -> intake.getReceivedAt() == null || !intake.getReceivedAt().isBefore(since))
                .toList();
        Set<String> done = dischargeProgress.doneReceptionIds(intakes.stream().map(ReceptionIntake::getId).toList());
        return intakes.stream()
                .filter(intake -> !done.contains(intake.getId()))
                .sorted(Comparator.comparing(ReceptionIntake::getReceivedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ReceptionIntake::getId))
                .toList();
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
        if (intake.isCancelled()) {
            // 취소된 접수를 등록 이벤트가 다시 살리지 않는다
            throw new ConflictException("reception already cancelled: " + request.getReceptionId());
        }

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
        recordKtasFromReception(saved, request);

        if (isNew) {
            // 접수는 거절하지 않고 항상 저장한다(응급 환자를 못 받는 상황이 생기면 안 된다). 같은 환자의 진행 중 접수가 있으면 경고만 남긴다.
            List<String> others = activeReceptionsOf(saved.getPatientId(), DEFAULT_ACTIVE_WINDOW_HOURS).stream()
                    .map(ReceptionIntake::getId).filter(id -> !id.equals(saved.getId())).toList();
            if (!others.isEmpty()) {
                log.warn("같은 환자의 진행 중인 응급 접수가 이미 있음 - patientId={}, 새 receptionId={}, 기존 receptionId={}",
                        saved.getPatientId(), saved.getId(), others);
            }
        }

        ReceptionIntakeDto dto = new ReceptionIntakeDto();
        dto.setReceptionId(saved.getId());
        dto.setPatientId(saved.getPatientId());
        dto.setArrivalPath(saved.getArrivalPathCode());
        dto.setReceivedAt(saved.getReceivedAt());
        dto.setMemo(saved.getMemo());
        dto.setChiefComplaintRaw(saved.getChiefComplaintRaw());
        return dto;
    }

    /** 접수가 입력한 KTAS 의 분류자 표시(접수 서비스가 한 분류라 직원 ID 가 없다) */
    static final String RECEPTION_ASSESSOR = "RECEPTION";

    /**
     * 접수에서 KTAS 등급을 같이 주면 최초(INITIAL) 분류로 저장한다 — 응급 목록에 접수 때 정한 등급이 바로 보이게.
     * 이미 최초 분류가 있으면(응급이 먼저 입력했거나 접수 이벤트를 다시 받은 경우) 건드리지 않는다.
     * 등급이 이상해도 접수 자체는 거절하지 않고 경고만 남긴다(응급 환자를 못 받는 상황이 생기면 안 된다).
     */
    private void recordKtasFromReception(ReceptionIntake saved, ReceptionIntakeCreateRequestDto request) {
        if (!StringUtils.hasText(request.getKtasLevel())) {
            return;
        }
        String level = request.getKtasLevel().trim();
        if (level.length() == 1 && Character.isDigit(level.charAt(0))) {
            level = "0" + level;                                   // 1 -> 01 (admin TRIAGE_CD 는 두 자리 코드)
        }
        if (!codeResolver.valueSet(EmgCodes.KTAS_LEVEL_GROUP, EmgCodes.KTAS_LEVEL_FALLBACK).contains(level)) {
            log.warn("접수의 KTAS 등급을 저장하지 못함 - 알 수 없는 값: receptionId={}, ktasLevel={}", saved.getId(), request.getKtasLevel());
            return;
        }
        if (triageAssessmentRepository.existsByReceptionIdAndAssessmentTypeCode(saved.getId(), EmgCodes.ASSESSMENT_INITIAL)) {
            return;
        }
        TriageAssessment assessment = new TriageAssessment();
        assessment.setReceptionId(saved.getId());
        assessment.setKtasLevelCode(level);
        assessment.setAssessmentTypeCode(EmgCodes.ASSESSMENT_INITIAL);
        assessment.setAssessedById(RECEPTION_ASSESSOR);
        assessment.setAssessedAt(request.getTriageDateTime() != null ? request.getTriageDateTime() : saved.getReceivedAt());
        assessment.setReason("Entered at reception");
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());
        triageAssessmentRepository.save(assessment);
        log.info("접수의 KTAS 등급 저장 - receptionId={}, level={}", saved.getId(), level);
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
