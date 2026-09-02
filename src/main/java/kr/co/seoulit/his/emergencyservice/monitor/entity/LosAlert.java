package kr.co.seoulit.his.emergencyservice.monitor.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "LOS_ALERT")
@Getter
@Setter
public class LosAlert {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "LOS_ALERT_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_NO", length = 36)
    private String receptionNo;

    @Column(name = "THRESHOLD_MINUTES")
    private Integer thresholdMinutes;

    @Column(name = "TRIGGERED_AT")
    private LocalDateTime triggeredAt;

    @Column(name = "ACKNOWLEDGED_BY_ID", length = 36)
    private String acknowledgedById;

    @Column(name = "ACKNOWLEDGED_AT")
    private LocalDateTime acknowledgedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
