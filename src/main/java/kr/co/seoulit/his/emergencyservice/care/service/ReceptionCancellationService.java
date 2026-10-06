package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionCancellableDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ClinicalNoteRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.ConsentRecordRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.CprEventRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.MedicationAdministrationRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.care.repository.TreatmentRecordRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.order.client.OrderCoreClient;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.EmsReferralRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.EwsRecordRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.IsolationAssessmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.RiskScreeningRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 접수 취소(RCP 의 ReceptionCancelled 이벤트) 반영.
 * 접수는 삭제하지 않고 취소 시각(CANCELLED_AT)만 남긴다. 이미 진료 기록이 있는 접수는 취소하지 않고 거절한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionCancellationService {

    public enum Result {
        /** 취소 처리함 */
        CANCELLED,
        /** 이미 취소된 접수(같은 이벤트가 다시 왔다) */
        ALREADY_CANCELLED,
        /** 응급에 없는 접수(등록 이벤트를 못 받았다) */
        NOT_FOUND,
        /** 진료 기록이 있어 취소하지 않음 */
        REFUSED_HAS_RECORDS,
        /** 처방코어에 처방이 있는지 확인하지 못해 취소하지 않음 */
        REFUSED_CANNOT_VERIFY
    }

    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final ClinicalNoteRepository clinicalNoteRepository;
    private final TreatmentRecordRepository treatmentRecordRepository;
    private final MedicationAdministrationRepository medicationAdministrationRepository;
    private final CprEventRepository cprEventRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final TriageAssessmentRepository triageAssessmentRepository;
    private final EwsRecordRepository ewsRecordRepository;
    private final IsolationAssessmentRepository isolationAssessmentRepository;
    private final RiskScreeningRepository riskScreeningRepository;
    private final EmsReferralRepository emsReferralRepository;
    private final BedAssignmentRepository bedAssignmentRepository;
    private final DispositionRepository dispositionRepository;
    private final OrderCoreClient orderCoreClient;

    /** 취소해도 되는지에 대한 판단 — 접수 취소 이벤트 처리와 취소 가능 여부 조회가 같은 기준을 쓴다 */
    private enum Verdict { CANCELLABLE, ALREADY_CANCELLED, NOT_FOUND, HAS_RECORDS, CANNOT_VERIFY }

    private record Evaluation(Verdict verdict, ReceptionIntake intake, List<String> records) {
    }

    public Result cancel(String receptionId, LocalDateTime cancelledAt) {
        Evaluation evaluation = evaluate(receptionId);
        switch (evaluation.verdict()) {
            case NOT_FOUND:
                log.warn("접수 취소 - 응급에 없는 접수라 건너뜀 receptionId={}", receptionId);
                return Result.NOT_FOUND;
            case ALREADY_CANCELLED:
                log.info("접수 취소 - 이미 취소된 접수 receptionId={}", receptionId);
                return Result.ALREADY_CANCELLED;
            case HAS_RECORDS:
                log.warn("접수 취소 거절 - 진료 기록이 있다 receptionId={}, 기록={}", receptionId, evaluation.records());
                return Result.REFUSED_HAS_RECORDS;
            case CANNOT_VERIFY:
                // 처방이 있는지 모르면 취소하지 않는다(처방이 달린 접수를 취소하는 쪽이 더 위험하다)
                log.warn("접수 취소 거절 - 처방코어에서 처방 여부를 확인하지 못함 receptionId={}", receptionId);
                return Result.REFUSED_CANNOT_VERIFY;
            default:
                break;
        }
        ReceptionIntake intake = evaluation.intake();
        LocalDateTime now = LocalDateTime.now();
        intake.setCancelledAt(cancelledAt != null ? cancelledAt : now);
        intake.setUpdatedAt(now);
        receptionIntakeRepository.save(intake);
        log.info("접수 취소 처리 receptionId={}, cancelledAt={}", receptionId, intake.getCancelledAt());
        return Result.CANCELLED;
    }

    /** 접수 서비스가 취소 전에 묻는 사전 안내 — 아무것도 바꾸지 않는다 */
    public ReceptionCancellableDto check(String receptionId) {
        if (receptionId == null || receptionId.isBlank()) {
            throw new IllegalArgumentException("receptionId is required");
        }
        Evaluation evaluation = evaluate(receptionId);
        ReceptionCancellableDto dto = new ReceptionCancellableDto();
        dto.setReceptionId(receptionId);
        dto.setReasonCode(evaluation.verdict().name());
        dto.setCancellable(evaluation.verdict() != Verdict.HAS_RECORDS && evaluation.verdict() != Verdict.CANNOT_VERIFY);
        dto.setRecords(evaluation.records());
        return dto;
    }

    private Evaluation evaluate(String receptionId) {
        ReceptionIntake intake = receptionId == null ? null : receptionIntakeRepository.findById(receptionId).orElse(null);
        if (intake == null) {
            return new Evaluation(Verdict.NOT_FOUND, null, List.of());
        }
        if (intake.isCancelled()) {
            return new Evaluation(Verdict.ALREADY_CANCELLED, intake, List.of());
        }
        List<String> records = recordsOf(receptionId);
        if (!records.isEmpty()) {
            return new Evaluation(Verdict.HAS_RECORDS, intake, records);
        }
        try {
            if (!orderCoreClient.listByReception(receptionId).isEmpty()) {
                return new Evaluation(Verdict.HAS_RECORDS, intake, List.of(RECORD_ORDER));
            }
        } catch (RuntimeException e) {
            log.warn("접수 취소 판단 - 처방코어에서 처방 여부를 확인하지 못함 receptionId={}, {}", receptionId, e.getMessage());
            return new Evaluation(Verdict.CANNOT_VERIFY, intake, List.of());
        }
        return new Evaluation(Verdict.CANCELLABLE, intake, List.of());
    }

    private static final String RECORD_ORDER = "ORDER";

    /**
     * 사람이 남긴 진료 기록의 종류(응급 DB 기준, 처방은 따로 본다). 접수가 넣어준 KTAS(RECEPTION)와 자동 알림은 기록으로 보지 않는다.
     * 앞의 코드는 응답에 그대로 나간다.
     */
    private List<String> recordsOf(String id) {
        List<String> found = new ArrayList<>();
        if (clinicalNoteRepository.existsByReceptionId(id)) found.add("CLINICAL_NOTE");
        if (treatmentRecordRepository.existsByReceptionId(id)) found.add("TREATMENT");
        if (medicationAdministrationRepository.existsByReceptionId(id)) found.add("MEDICATION");
        if (cprEventRepository.existsByReceptionId(id)) found.add("CPR");
        if (consentRecordRepository.existsByReceptionId(id)) found.add("CONSENT");
        if (triageAssessmentRepository.findByReceptionId(id).stream()
                .anyMatch(a -> !CareServiceImpl.RECEPTION_ASSESSOR.equals(a.getAssessedById()))) found.add("KTAS");
        if (ewsRecordRepository.existsByReceptionId(id)) found.add("VITAL_SIGNS");
        if (isolationAssessmentRepository.existsByReceptionId(id)) found.add("ISOLATION");
        if (riskScreeningRepository.existsByReceptionId(id)) found.add("RISK_SCREENING");
        if (emsReferralRepository.existsByReceptionId(id)) found.add("EMS_REFERRAL");
        if (bedAssignmentRepository.existsByReceptionId(id)) found.add("BED_ASSIGNMENT");
        if (dispositionRepository.existsByReceptionId(id)) found.add("DISPOSITION");
        return found;
    }
}
