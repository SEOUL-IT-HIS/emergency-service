package kr.co.seoulit.his.emergencyservice.resource.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EQUIPMENT")
@Getter
@Setter
public class Equipment {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "EQUIPMENT_ID", length = 36)
    private String id;

    @Column(name = "ASSET_NO", length = 20, unique = true)
    private String assetNo;

    @Column(name = "EQUIPMENT_TYPE_CODE", length = 20)
    private String equipmentTypeCode;

    @Column(name = "EQUIPMENT_STATUS_CODE", length = 20)
    private String equipmentStatusCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
