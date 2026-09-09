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

    @Column(name = "ARRIVAL_PATH", length = 50)
    private String arrivalPath;

    @Column(name = "RECEIVED_AT")
    private LocalDateTime receivedAt;

    @Column(name = "MEMO", length = 500)
    private String memo;

    @Column(name = "CHIEF_COMPLAINT_RAW", length = 500)
    private String chiefComplaintRaw;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
