package kr.co.seoulit.his.emergencyservice.disposition.service;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.util.InChunks;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 접수 건별 퇴실 진행 단계 — 최신 퇴실 결정과 그 후속 조치(병동 회신·전원 소견서)로 계산한다(저장값 아님).
 * 환자 목록 상태 필터, 현황판 재실 환자 수, 장기체류 알림, 퇴실 결정 변경 가능 여부가 모두 이 기준 하나를 쓴다.
 */
@Component
@RequiredArgsConstructor
public class DischargeProgress {

    public enum Stage {
        /** 퇴실 결정 없음 */
        NONE,
        /** 결정했지만 후속 조치 전(입원요청 없음·거부됨, 전원 소견서 없음) — 결정을 바꿀 수 있다 */
        OPEN,
        /** 병동 회신 대기 중(입원요청 요청됨) — 병동에 요청이 가 있으므로 결정을 바꿀 수 없다 */
        WAITING_WARD,
        /** 퇴실 처리 완료: 귀가·사망·자의퇴원은 결정 즉시, 입원은 병상 배정, 전원은 소견서 작성 */
        DONE
    }

    private final DispositionRepository dispositionRepository;
    private final AdmissionRequestRepository admissionRequestRepository;
    private final TransferNoteRepository transferNoteRepository;
    private final ReceptionIntakeRepository receptionIntakeRepository;

    /** 접수 건별 단계. 퇴실 결정이 없는 접수는 NONE */
    public Map<String, Stage> stages(Collection<String> receptionIds) {
        Map<String, Stage> result = new HashMap<>();
        receptionIds.forEach(id -> result.put(id, Stage.NONE));
        if (receptionIds.isEmpty()) {
            return result;
        }
        Map<String, Disposition> latest = new HashMap<>();
        for (Disposition d : InChunks.query(receptionIds, dispositionRepository::findByReceptionIdIn)) {
            Disposition cur = latest.get(d.getReceptionId());
            if (cur == null || isAfter(d.getDecidedAt(), cur.getDecidedAt())) {
                latest.put(d.getReceptionId(), d);
            }
        }
        if (latest.isEmpty()) {
            return result;
        }
        List<String> dispositionIds = latest.values().stream().map(Disposition::getId).collect(Collectors.toList());
        // 입원요청은 가장 최근 요청의 상태로 본다
        Map<String, AdmissionRequest> latestRequest = new HashMap<>();
        for (AdmissionRequest a : InChunks.query(dispositionIds, admissionRequestRepository::findByDispositionIdIn)) {
            AdmissionRequest cur = latestRequest.get(a.getDisposition().getId());
            if (cur == null || isAfter(a.getRequestedAt(), cur.getRequestedAt())) {
                latestRequest.put(a.getDisposition().getId(), a);
            }
        }
        Set<String> withTransferNote = InChunks.query(dispositionIds, transferNoteRepository::findByDispositionIdIn).stream()
                .map(n -> n.getDisposition().getId()).collect(Collectors.toSet());

        latest.forEach((receptionId, d) -> {
            String type = d.getDispositionTypeCode();
            Stage stage;
            if (EmgCodes.DISPOSITION_ADMIT.equals(type)) {
                AdmissionRequest request = latestRequest.get(d.getId());
                String status = request == null ? null : request.getRequestStatusCode();
                stage = EmgCodes.ADMISSION_BED_ASSIGNED.equals(status) ? Stage.DONE
                        : EmgCodes.ADMISSION_REQUESTED.equals(status) ? Stage.WAITING_WARD
                        : Stage.OPEN;
            } else if (EmgCodes.DISPOSITION_TRANSFER.equals(type)) {
                stage = withTransferNote.contains(d.getId()) ? Stage.DONE : Stage.OPEN;
            } else {
                stage = Stage.DONE;
            }
            result.put(receptionId, stage);
        });
        return result;
    }

    public Stage stage(String receptionId) {
        return stages(List.of(receptionId)).get(receptionId);
    }

    /**
     * 퇴실 처리가 끝난(DONE) 접수 또는 접수에서 취소한 접수에 새로 배치·평가·처방하려는 요청을 막는다 → ConflictException(409).
     * 병상 배정, KTAS·활력징후·격리·위험 스크리닝 등록, 처방 등록에 쓴다. 진료기록·처치·투약·CPR·동의 같은 사후 기록과
     * 해제·취소 같은 정리 작업은 퇴실 뒤에도 필요하므로 이 검사를 걸지 않는다. 병동 대기(WAITING_WARD) 중은 아직 응급실에 있으니 허용한다.
     */
    public void requireNotDischarged(String receptionId) {
        if (receptionId == null || receptionId.isBlank()) {
            return;
        }
        // 접수에서 취소한 접수에도 새로 입력하지 않는다
        if (receptionIntakeRepository.findById(receptionId).filter(ReceptionIntake::isCancelled).isPresent()) {
            throw new ConflictException("reception cancelled: " + receptionId);
        }
        if (stage(receptionId) == Stage.DONE) {
            throw new ConflictException("reception already discharged: " + receptionId);
        }
    }

    public Set<String> doneReceptionIds(Collection<String> receptionIds) {
        return stages(receptionIds).entrySet().stream()
                .filter(e -> e.getValue() == Stage.DONE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static boolean isAfter(java.time.LocalDateTime a, java.time.LocalDateTime b) {
        return a != null && (b == null || a.isAfter(b));
    }
}
