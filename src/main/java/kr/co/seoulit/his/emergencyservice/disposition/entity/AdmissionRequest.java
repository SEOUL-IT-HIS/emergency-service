package kr.co.seoulit.his.emergencyservice.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "ADMISSION_REQUEST")
@Getter
@Setter
public class AdmissionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ADMISSION_REQUEST_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "TARGET_DEPT_CODE", length = 20)
    private String targetDeptCode;

    @Column(name = "REQUEST_STATUS_CODE", length = 20)
    private String requestStatusCode;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
