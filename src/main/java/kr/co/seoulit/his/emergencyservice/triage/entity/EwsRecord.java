package kr.co.seoulit.his.emergencyservice.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EWS_RECORD")
@Getter
@Setter
public class EwsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EWS_RECORD_ID")
    private String id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "SYSTOLIC_BP")
    private Integer systolicBp;

    @Column(name = "HEART_RATE")
    private Integer heartRate;

    @Column(name = "RESP_RATE")
    private Integer respRate;

    @Column(name = "TEMPERATURE", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "SPO2")
    private Integer spo2;

    @Column(name = "GCS")
    private Integer gcs;

    @Column(name = "EWS_SCORE", precision = 5, scale = 2)
    private BigDecimal ewsScore;

    @Column(name = "MEASURED_BY_ID", length = 36)
    private String measuredById;

    @Column(name = "MEASURED_AT")
    private LocalDateTime measuredAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
