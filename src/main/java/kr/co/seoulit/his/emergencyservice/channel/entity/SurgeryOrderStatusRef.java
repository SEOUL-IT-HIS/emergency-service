package kr.co.seoulit.his.emergencyservice.channel.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// 수술·시술 긴급 요청(UC-ORD-10, SPLIT): 생성은 GR2 코어, 실행은 SUR 라우팅.
// 응급은 상태만 수신하는 참조 테이블이라 orderId 기준 upsert로 채워진다(요청을 응급이 직접 만들지 않음).
@Entity
@Table(schema = "EMERGENCY", name = "SURGERY_ORDER_STATUS_REF",
        uniqueConstraints = @UniqueConstraint(name = "UK_SURGERY_ORDER_STATUS_REF_ORDER", columnNames = "ORDER_ID"))
@Getter
@Setter
public class SurgeryOrderStatusRef {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "SURGERY_ORDER_STATUS_REF_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_ID", length = 36)
    private String receptionId;

    @Column(name = "ORDER_ID", length = 36, nullable = false)
    private String orderId;

    @Column(name = "PROCEDURE_CODE", length = 30)
    private String procedureCode;

    @Column(name = "REQUEST_STATUS_CODE", length = 20)
    private String requestStatusCode;

    @Column(name = "STATUS_RECEIVED_AT")
    private LocalDateTime statusReceivedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
