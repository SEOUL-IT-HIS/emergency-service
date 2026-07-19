package kr.co.seoulit.his.emergencyservice.channel.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "CONSULT_REQUEST")
@Getter
@Setter
public class ConsultRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CONSULT_REQUEST_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "TARGET_DEPT_CODE", length = 20)
    private String targetDeptCode;

    @Column(name = "CONSULT_STATUS_CODE", length = 20)
    private String consultStatusCode;

    @Column(name = "REASON", length = 4000)
    private String reason;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    @Column(name = "REPLIED_AT")
    private LocalDateTime repliedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
