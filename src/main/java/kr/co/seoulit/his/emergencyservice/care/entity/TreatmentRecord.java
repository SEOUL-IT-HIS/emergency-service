package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "TREATMENT_RECORD")
@Getter
@Setter
public class TreatmentRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "TREATMENT_RECORD_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_ID", length = 36)
    private String receptionId;

    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "TREATMENT_TYPE_CODE", length = 20)
    private String treatmentTypeCode;

    @Column(name = "DESCRIPTION", length = 4000)
    private String description;

    @Column(name = "PERFORMED_BY_ID", length = 36)
    private String performedById;

    @Column(name = "PERFORMED_AT")
    private LocalDateTime performedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
