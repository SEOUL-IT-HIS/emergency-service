package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 응급 → 접수 "오접수·중복 접수로 닫음" 이벤트(ReceptionVoided). 접수 서비스가 받아 해당 접수를 응급 기록 유무와 상관없이 취소한다
 * (접수팀 합의 2026-10-08). 토픽 emergency.reception.voided.v1, 메시지 key 는 receptionId.
 * 필드는 평평하게 두고 version·source 는 기존 접수 이벤트와 맞춘다. occurredAt 은 응급이 닫은(퇴실 결정) 시각의 ISO-8601 문자열(오프셋 포함, 초 단위).
 * 재전송할 때도 occurredAt 은 처음 결정 시각 그대로이고 eventId 만 새로 만든다(접수는 같은 접수를 두 번 받아도 안전하게 처리한다).
 * reasonType 은 DUPLICATE(중복 접수) / ERRONEOUS(접수 오류), keptReceptionId 는 중복일 때 남기는 접수 ID(없으면 null),
 * decidedBy 는 닫은 의사의 직원 ID(36자 이내).
 */
public record ReceptionVoidedEvent(
        String eventId,
        String eventType,
        String version,
        String source,
        String occurredAt,
        String receptionId,
        String patientId,
        String reasonType,
        String reasonText,
        String keptReceptionId,
        String decidedBy) {

    public static final String EVENT_TYPE = "ReceptionVoided";
    public static final String VERSION = "1.0";
    public static final String SOURCE = "ER";
    /** 응급 서버 시각은 한국 시간이다 — 오프셋을 붙여 접수가 시간대를 헷갈리지 않게 한다 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public static ReceptionVoidedEvent of(Disposition disposition, String patientId) {
        String occurredAt = disposition.getDecidedAt() == null ? null
                : disposition.getDecidedAt().atZone(ZONE).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new ReceptionVoidedEvent(UUID.randomUUID().toString(), EVENT_TYPE, VERSION, SOURCE, occurredAt,
                disposition.getReceptionId(), patientId, disposition.getVoidReasonTypeCode(), disposition.getVoidReason(),
                disposition.getKeptReceptionId(), disposition.getDecidedById());
    }
}
