package kr.co.seoulit.his.emergencyservice.triage.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "ISOLATION_ASSESSMENT")
@Getter
@Setter
public class IsolationAssessment {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "ISOLATION_ASSESSMENT_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ISOLATION_TYPE_CODE", length = 20)
    private String isolationTypeCode;

    @Column(name = "REQUIRED_YN", length = 1)
    private String requiredYn;

    @Column(name = "DECIDED_BY_ID", length = 36)
    private String decidedById;

    @Column(name = "DECIDED_AT")
    private LocalDateTime decidedAt;

    @Column(name = "RELEASED_AT")
    private LocalDateTime releasedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
