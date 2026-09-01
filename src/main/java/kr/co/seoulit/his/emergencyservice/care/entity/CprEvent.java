package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(schema = "EMERGENCY", name = "CPR_EVENT")
@Getter
@Setter
public class CprEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "CPR_EVENT_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_NO", length = 36)
    private String receptionNo;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt;

    @Column(name = "OUTCOME_CODE", length = 20)
    private String outcomeCode;

    @OneToMany(mappedBy = "cprEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CprTimeline> timelines = new ArrayList<>();

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
