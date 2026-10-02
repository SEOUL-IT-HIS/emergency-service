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
}
