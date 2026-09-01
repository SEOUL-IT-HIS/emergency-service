package kr.co.seoulit.his.emergencyservice.channel.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "SURGERY_REQUEST")
@Getter
@Setter
public class SurgeryRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "SURGERY_REQUEST_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_NO", length = 36)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "PROCEDURE_CODE", length = 30)
    private String procedureCode;

    @Column(name = "REQUEST_STATUS_CODE", length = 20)
    private String requestStatusCode;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
