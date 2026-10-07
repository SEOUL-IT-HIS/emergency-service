package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "RECEPTION_INTAKE")
@Getter
@Setter
public class ReceptionIntake {

    @Id
    @Column(name = "RECEPTION_ID", length = 36)
    private String id;

    @Column(name = "PATIENT_ID", length = 36)
    private String patientId;

    @Column(name = "PATIENT_NAME", length = 100)
    private String patientName;

    @Column(name = "ARRIVAL_PATH_CODE", length = 50)
    private String arrivalPathCode;

    @Column(name = "RECEIVED_AT")
    private LocalDateTime receivedAt;

    @Column(name = "MEMO", length = 500)
    private String memo;

    @Column(name = "CHIEF_COMPLAINT_RAW", length = 500)
    private String chiefComplaintRaw;

    // 접수 취소 시각 - 값이 있으면 취소된 접수(삭제하지 않고 상태로 남긴다). RCP 의 ReceptionCancelled 이벤트로 채워진다.
    @Column(name = "CANCELLED_AT")
    private LocalDateTime cancelledAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    public boolean isCancelled() {
        return cancelledAt != null;
    }
}
