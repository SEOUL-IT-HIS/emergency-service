package kr.co.seoulit.his.emergencyservice.care.messaging.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReceptionIntakeEvent {
    // 메타필드 - RCP가 채워서 보냄
    private String eventId;           //  멱읃성 체크/추적용
    private LocalDateTime occurredAt; // RCP에서 이벤트 발생한 시간

    // 실제 데이터 (기존 REST DTO와 동일필드)
    private String receptionId;
    private String patientId;
    private String arrivalPath;
    private LocalDateTime receivedAt;
    private String memo;
    private String chiefComplaintRaw;
    // 접수에서 입력한 KTAS 등급·분류 시각(선택)
    @com.fasterxml.jackson.annotation.JsonAlias({"ktasLevelCode", "ktasScore", "ktas"})
    private String ktasLevel;
    private LocalDateTime triageDateTime;

    // 취소 이벤트 구분(등록 이벤트에는 없음) - eventType "ReceptionCancelled", status "CANCELLED"
    public static final String TYPE_CANCELLED = "ReceptionCancelled";
    private String eventType;
    private String status;

    /** 접수 취소 이벤트인지 - eventType 이 없으면 등록이다 */
    public boolean isCancellation() {
        return eventType != null && TYPE_CANCELLED.equalsIgnoreCase(eventType.trim());
    }
}
