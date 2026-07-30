package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "MEDICATION_ADMINISTRATION")
@Getter
@Setter
public class MedicationAdministration {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "MEDICATION_ADMINISTRATION_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "ORDER_ID", nullable = false)
    private Long orderId;

    @Column(name = "ORDER_ITEM_ID")
    private Long orderItemId;

    @Column(name = "DRUG_CODE", length = 20)
    private String drugCode;

    @Column(name = "DOSE", length = 30)
    private String dose;

    @Column(name = "ROUTE_CODE", length = 10)
    private String routeCode;

    @Column(name = "ADMINISTERED_BY_ID", length = 36)
    private String administeredById;

    @Column(name = "ADMINISTERED_AT")
    private LocalDateTime administeredAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
