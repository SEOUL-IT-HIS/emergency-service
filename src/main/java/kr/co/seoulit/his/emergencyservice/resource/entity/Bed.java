package kr.co.seoulit.his.emergencyservice.resource.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "BED")
@Getter
@Setter
public class Bed {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "BED_ID", length = 36)
    private String id;

    @Column(name = "BED_NO", length = 20, unique = true)
    private String bedNo;

    @Column(name = "ZONE_CODE", length = 20)
    private String zoneCode;

    @Column(name = "BED_TYPE_CODE", length = 20)
    private String bedTypeCode;

    @Column(name = "BED_STATUS_CODE", length = 20)
    private String bedStatusCode;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
