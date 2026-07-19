package kr.co.seoulit.his.emergencyservice.triage.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import kr.co.seoulit.his.emergencyservice.triage.entity.*;
import kr.co.seoulit.his.emergencyservice.triage.mapper.TriageMapstructMapper;
import kr.co.seoulit.his.emergencyservice.triage.repository.*;
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
                .orElseThrow(() -> ResourceNotFoundException.of("ktas", id));
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
