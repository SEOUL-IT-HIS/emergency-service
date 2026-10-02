package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * 동의 기록 — 종이로 받은 동의서의 "수령 사실"만 저장한다(서명·원본 파일은 저장하지 않음).
 * 한 번 기록하면 수정하지 않는 이력 테이블: 유예 후 사후 동의를 받으면 새 행을 추가한다.
 */
@Entity
@Table(schema = "EMERGENCY", name = "CONSENT_RECORD")
@Getter
@Setter
public class ConsentRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "CONSENT_RECORD_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_ID", length = 36, nullable = false)
    private String receptionId;

    @Column(name = "CONSENT_TYPE_CODE", length = 20, nullable = false)
    private String consentTypeCode;

    @Column(name = "CONSENT_STATUS_CODE", length = 20, nullable = false)
    private String consentStatusCode;

    @Column(name = "CONSENTED_BY_CODE", length = 20, nullable = false)
    private String consentedByCode;

    @Column(name = "CONSENTER_NAME", length = 100)
    private String consenterName;

    @Column(name = "REASON", length = 500)
    private String reason;

    @Column(name = "RECEIVED_AT", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "RECORDED_BY_ID", length = 36, nullable = false)
    private String recordedById;

    @Column(name = "RECORDED_AT", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
