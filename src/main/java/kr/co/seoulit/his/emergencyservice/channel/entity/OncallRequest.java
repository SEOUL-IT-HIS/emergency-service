package kr.co.seoulit.his.emergencyservice.channel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "ONCALL_REQUEST")
@Getter
@Setter
public class OncallRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ONCALL_REQUEST_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "TARGET_ROLE_CODE", length = 20)
    private String targetRoleCode;

    @Column(name = "CALLED_BY_ID", length = 36)
    private String calledById;

    @Column(name = "CALLED_AT")
    private LocalDateTime calledAt;

    @Column(name = "RESPONDED_AT")
    private LocalDateTime respondedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
