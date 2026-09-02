package kr.co.seoulit.his.emergencyservice.triage.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EMS_REFERRAL")
@Getter
@Setter
public class EmsReferral {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "EMS_REFERRAL_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_NO", length = 36)
    private String receptionNo;

    @Column(name = "EMS_AGENCY_NAME", length = 100)
    private String emsAgencyName;

    @Column(name = "VITALS_ON_SCENE", length = 4000)
    private String vitalsOnScene;

    @Column(name = "PREHOSPITAL_TREATMENT", length = 4000)
    private String prehospitalTreatment;

    @Column(name = "TRANSMITTED_AT")
    private LocalDateTime transmittedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
