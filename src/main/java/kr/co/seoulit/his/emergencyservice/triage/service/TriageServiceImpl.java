package kr.co.seoulit.his.emergencyservice.triage.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import kr.co.seoulit.his.emergencyservice.triage.entity.*;
import kr.co.seoulit.his.emergencyservice.triage.mapper.TriageMapstructMapper;
import kr.co.seoulit.his.emergencyservice.triage.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TriageServiceImpl implements TriageService {

    private static final Set<String> VALID_KTAS_SCORES = Set.of("1", "2", "3", "4", "5");
    private static final Set<String> VALID_ISOLATION_YN = Set.of("Y", "N");
    private static final Set<String> VALID_SCREEN_TYPES = Set.of("SEPSIS", "STROKE");
    private static final Set<String> VALID_SCREEN_RESULTS = Set.of("NEGATIVE", "POSITIVE", "INCONCLUSIVE");

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
    @Transactional(readOnly = true)
    public List<TriageAssessmentDto> getKtasHistory(String receptionNo) {
        if (!StringUtils.hasText(receptionNo)) {
            throw new IllegalArgumentException("receptionNo is required");
        }
        return triageMapper.toKtasDtoList(
                triageAssessmentRepository.findByReceptionNoOrderByAssessedAtAsc(receptionNo));
    }

    @Override
    @Transactional
    public TriageAssessmentDto createKtas(KtasCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getKtasScore())) {
            throw new IllegalArgumentException("encounterId and ktasScore are required");
        }
        if (!VALID_KTAS_SCORES.contains(request.getKtasScore())) {
            throw new IllegalArgumentException("ktasScore must be one of 1~5");
        }
        String assessmentTypeCode =
                StringUtils.hasText(request.getAssessmentTypeCode()) ? request.getAssessmentTypeCode() : "INITIAL";
        if ("INITIAL".equals(assessmentTypeCode)
                && triageAssessmentRepository.existsByReceptionNoAndAssessmentTypeCode(
                        request.getEncounterId(), "INITIAL")) {
            // 이미 최초 분류(INITIAL)가 등록된 접수 건입니다
            throw new ConflictException("Initial KTAS classification already registered for this encounter: " + request.getEncounterId());
        }

        TriageAssessment entity = new TriageAssessment();
        entity.setReceptionNo(request.getEncounterId());
        entity.setKtasLevelCode(request.getKtasScore());
        entity.setAssessmentTypeCode(assessmentTypeCode);
        entity.setAssessedById(request.getAssessedById());
        entity.setAssessedAt(LocalDateTime.now());
        entity.setReason(request.getReason());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toKtasDto(triageAssessmentRepository.save(entity));
    }

    /**
     * KTAS 재평가 — 기존 분류(id)를 덮어쓰지 않고 REASSESS 이력을 새 행으로 추가한다.
     * (UD2-43/UD2-59: "이력으로 추가 저장" — PUT 경로/id는 "무엇을 재평가하는지" 참조용이고,
     * 실제로는 새 TRIAGE_ASSESSMENT 행을 INSERT 하여 이전 등급과 비교할 수 있게 한다.)
     */
    @Override
    @Transactional
    public TriageAssessmentDto updateKtas(String id, KtasUpdateRequestDto request) {
        TriageAssessment previous = triageAssessmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ktas", id));

        String nextScore = StringUtils.hasText(request.getKtasScore())
                ? request.getKtasScore() : previous.getKtasLevelCode();
        if (!VALID_KTAS_SCORES.contains(nextScore)) {
            throw new IllegalArgumentException("ktasScore must be one of 1~5");
        }

        TriageAssessment entity = new TriageAssessment();
        entity.setReceptionNo(previous.getReceptionNo());
        entity.setKtasLevelCode(nextScore);
        entity.setAssessmentTypeCode("REASSESS");
        entity.setAssessedById(request.getAssessedById() != null ? request.getAssessedById() : previous.getAssessedById());
        entity.setReason(request.getReason());
        entity.setAssessedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toKtasDto(triageAssessmentRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EwsRecordDto> getVitalAssessments(String receptionNo) {
        if (!StringUtils.hasText(receptionNo)) {
            throw new IllegalArgumentException("receptionNo is required");
        }
        return triageMapper.toEwsDtoList(ewsRecordRepository.findByReceptionNo(receptionNo));
    }

    @Override
    @Transactional
    public List<EwsRecordDto> createVitalAssessments(VitalAssessmentCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || request.getVitals() == null || request.getVitals().isEmpty()) {
            throw new IllegalArgumentException("encounterId and vitals[] are required");
        }
        List<EwsRecord> saved = new ArrayList<>();
        for (VitalAssessmentCreateRequestDto.VitalItemDto vital : request.getVitals()) {
            validateVitalItem(vital);
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

    /**
     * 활력징후 범위 검증 (UD2-66/67).
     * EWS 점수 자동 산출 정책은 아직 미확정 — 서버는 클라이언트가 넘긴 ewsScore를 그대로 저장하고,
     * 여기서는 생리적으로 불가능한 값만 걸러낸다.
     */
    private void validateVitalItem(VitalAssessmentCreateRequestDto.VitalItemDto vital) {
        if (vital.getSystolicBp() != null && (vital.getSystolicBp() < 0 || vital.getSystolicBp() > 300)) {
            throw new IllegalArgumentException("systolicBp must be between 0 and 300");
        }
        if (vital.getHeartRate() != null && (vital.getHeartRate() < 0 || vital.getHeartRate() > 300)) {
            throw new IllegalArgumentException("heartRate must be between 0 and 300");
        }
        if (vital.getRespRate() != null && (vital.getRespRate() < 0 || vital.getRespRate() > 100)) {
            throw new IllegalArgumentException("respRate must be between 0 and 100");
        }
        if (vital.getSpo2() != null && (vital.getSpo2() < 0 || vital.getSpo2() > 100)) {
            throw new IllegalArgumentException("spo2 must be between 0 and 100");
        }
        if (vital.getGcs() != null && (vital.getGcs() < 3 || vital.getGcs() > 15)) {
            throw new IllegalArgumentException("gcs must be between 3 and 15");
        }
        if (vital.getTemperature() != null
                && (vital.getTemperature().compareTo(new BigDecimal("20.0")) < 0
                        || vital.getTemperature().compareTo(new BigDecimal("45.0")) > 0)) {
            throw new IllegalArgumentException("temperature must be between 20.0 and 45.0");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<IsolationAssessmentDto> getIsolations(String receptionNo) {
        if (!StringUtils.hasText(receptionNo)) {
            throw new IllegalArgumentException("receptionNo is required");
        }
        return triageMapper.toIsolationDtoList(isolationAssessmentRepository.findByReceptionNo(receptionNo));
    }

    @Override
    @Transactional
    public IsolationAssessmentDto createIsolation(IsolationCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) && !StringUtils.hasText(request.getPatientId())) {
            throw new IllegalArgumentException("patientId or encounterId is required");
        }
        if (!StringUtils.hasText(request.getIsolationTypeCode())) {
            throw new IllegalArgumentException("isolationTypeCode is required");
        }
        String requiredYn = StringUtils.hasText(request.getRequiredYn()) ? request.getRequiredYn() : "Y";
        if (!VALID_ISOLATION_YN.contains(requiredYn)) {
            throw new IllegalArgumentException("requiredYn must be Y or N");
        }
        String receptionNo = StringUtils.hasText(request.getEncounterId())
                ? request.getEncounterId() : request.getPatientId();

        boolean hasActiveIsolation = isolationAssessmentRepository.findByReceptionNo(receptionNo).stream()
                .anyMatch(existing -> existing.getReleasedAt() == null
                        && existing.getIsolationTypeCode().equals(request.getIsolationTypeCode()));
        if (hasActiveIsolation) {
            // 이미 활성 상태인 격리가 있습니다
            throw new ConflictException(
                    "An active isolation (" + request.getIsolationTypeCode() + ") already exists: " + receptionNo);
        }

        IsolationAssessment entity = new IsolationAssessment();
        entity.setReceptionNo(receptionNo);
        entity.setIsolationTypeCode(request.getIsolationTypeCode());
        entity.setRequiredYn(requiredYn);
        entity.setDecidedById(request.getDecidedById());
        entity.setDecidedAt(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toIsolationDto(isolationAssessmentRepository.save(entity));
    }

    @Override
    @Transactional
    public IsolationAssessmentDto releaseIsolation(String id) {
        IsolationAssessment entity = isolationAssessmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("isolation", id));
        if (entity.getReleasedAt() != null) {
            // 이미 해제된 격리입니다
            throw new ConflictException("This isolation has already been released: " + id);
        }
        entity.setReleasedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return triageMapper.toIsolationDto(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskScreeningDto> getRiskScreenings(String receptionNo) {
        if (!StringUtils.hasText(receptionNo)) {
            throw new IllegalArgumentException("receptionNo is required");
        }
        return triageMapper.toRiskDtoList(riskScreeningRepository.findByReceptionNo(receptionNo));
    }

    @Override
    @Transactional
    public RiskScreeningDto createRiskScreening(RiskScreeningCreateRequestDto request) {
        if (!StringUtils.hasText(request.getEncounterId()) || !StringUtils.hasText(request.getScreenType())) {
            throw new IllegalArgumentException("encounterId and screenType are required");
        }
        if (!VALID_SCREEN_TYPES.contains(request.getScreenType())) {
            throw new IllegalArgumentException("screenType must be one of SEPSIS, STROKE");
        }
        if (StringUtils.hasText(request.getResultCode()) && !VALID_SCREEN_RESULTS.contains(request.getResultCode())) {
            throw new IllegalArgumentException("resultCode must be one of NEGATIVE, POSITIVE, INCONCLUSIVE");
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
