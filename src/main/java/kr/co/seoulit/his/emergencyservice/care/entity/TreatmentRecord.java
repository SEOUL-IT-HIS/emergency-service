package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "TREATMENT_RECORD")
@Getter
@Setter
public class TreatmentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TREATMENT_RECORD_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

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
