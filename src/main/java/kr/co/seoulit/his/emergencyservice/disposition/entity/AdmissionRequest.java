package kr.co.seoulit.his.emergencyservice.disposition.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "ADMISSION_REQUEST")
@Getter
@Setter
public class AdmissionRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "ADMISSION_REQUEST_ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "TARGET_DEPT_CODE", length = 20)
    private String targetDeptCode;

    @Column(name = "REQUEST_STATUS_CODE", length = 20)
    private String requestStatusCode;

    /** 병동이 실제로 배정한 병동(BED_ASSIGNED 회신의 wardCode, WARD_CD 값). 희망 병동(wardPref)과 다를 수 있다 */
    @Column(name = "ASSIGNED_WARD_CODE", length = 20)
    private String assignedWardCode;

    @Column(name = "REQUESTED_AT")
    private LocalDateTime requestedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
