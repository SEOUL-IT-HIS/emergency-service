package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "CPR_TIMELINE")
@Getter
@Setter
public class CprTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CPR_TIMELINE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CPR_EVENT_ID", nullable = false)
    private CprEvent cprEvent;

    @Column(name = "EVENT_AT")
    private LocalDateTime eventAt;

    @Column(name = "EVENT_TYPE_CODE", length = 20)
    private String eventTypeCode;

    @Column(name = "DETAIL", length = 4000)
    private String detail;

    @Column(name = "RECORDED_BY_ID", length = 36)
    private String recordedById;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
