package kr.co.seoulit.his.emergencyservice.triage.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "TRIAGE_ASSESSMENT")
@Getter
@Setter
public class TriageAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRIAGE_ASSESSMENT_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "KTAS_LEVEL_CODE", length = 10)
    private String ktasLevelCode;

    @Column(name = "ASSESSMENT_TYPE_CODE", length = 20)
    private String assessmentTypeCode;

    @Column(name = "ASSESSED_BY_ID", length = 36)
    private String assessedById;

    @Column(name = "ASSESSED_AT")
    private LocalDateTime assessedAt;

    @Column(name = "REASON", length = 4000)
    private String reason;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
