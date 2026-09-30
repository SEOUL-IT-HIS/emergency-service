package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import java.time.LocalDateTime;

/**
 * 응급 → 병동 입원요청 이벤트(ADMISSION_REQUESTED). 규격은 docs/flow.md 7-1장 / 병동팀 확인 메시지 기준.
 * dispositionId 가 요청·회신을 잇는 키다(병동이 회신에 그대로 돌려준다).
 * 응급에는 진단 데이터가 없어 diagnosis 는 뺐다. patientId·isolationYn 은 발행 시점에 접수·격리평가에서 조회해 채운다.
 */
public record AdmissionRequestedEvent(
        String dispositionId,
        String encounterId,
        String patientId,
        String targetDeptCode,
        String wardPref,
        String isolationYn,
        String requestedBy,
        LocalDateTime requestedAt) {
}
