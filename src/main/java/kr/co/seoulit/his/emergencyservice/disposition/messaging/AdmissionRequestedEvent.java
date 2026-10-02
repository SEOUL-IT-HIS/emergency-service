package kr.co.seoulit.his.emergencyservice.disposition.messaging;

/**
 * 응급 → 병동 입원요청 이벤트(ADMISSION_REQUESTED). 규격은 docs/flow.md 7-1장 / 병동팀 확인 메시지 기준.
 * dispositionId 가 요청·회신을 잇는 키다(병동이 회신에 그대로 돌려준다).
 * 응급에는 진단 데이터가 없어 diagnosis 대신 선택 필드 note(요청 메모, 자유 텍스트)만 둔다(병동팀 합의 2026-09-30).
 * admissionRequestId 는 요청 1건마다 새로 만들어지는 ID다. 같은 dispositionId 로 다시 요청(거부 뒤 재요청)해도 달라서,
 * 병동이 회신에 그대로 돌려주면 응급이 어느 요청에 대한 회신인지 정확히 구분한다(옛 회신이 새 요청을 덮어쓰지 않는다).
 * requestedBy 는 응급에서 퇴실을 결정한 의사 ID다(입원 후 주치의가 아닐 수 있어 병동이 주치의를 따로 정한다).
 * requestedAt 은 병동팀 합의 형식 "yyyy-MM-dd'T'HH:mm:ss"(소수 초 없음) 문자열이다. 메시지 key 는 dispositionId. patientId·isolationYn 은 발행 시점에 접수·격리평가에서 조회해 채운다.
 */
public record AdmissionRequestedEvent(
        String dispositionId,
        String admissionRequestId,
        String encounterId,
        String patientId,
        String targetDeptCode,
        String wardPref,
        String isolationYn,
        String requestedBy,
        String requestedAt,
        String note) {
}
