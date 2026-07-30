package kr.co.seoulit.his.emergencyservice.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "RISK_SCREENING")
@Getter
@Setter
public class RiskScreening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RISK_SCREENING_ID")
    private String id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "SCREENING_TYPE_CODE", length = 20)
    private String screeningTypeCode;

    @Column(name = "SCORE", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "RESULT_CODE", length = 20)
    private String resultCode;

    @Column(name = "SCREENED_BY_ID", length = 36)
    private String screenedById;

    @Column(name = "SCREENED_AT")
    private LocalDateTime screenedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
