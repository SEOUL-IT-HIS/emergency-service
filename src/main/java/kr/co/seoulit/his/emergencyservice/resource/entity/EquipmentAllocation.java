package kr.co.seoulit.his.emergencyservice.resource.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "EQUIPMENT_ALLOCATION")
@Getter
@Setter
public class EquipmentAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EQUIPMENT_ALLOCATION_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EQUIPMENT_ID", nullable = false)
    private Equipment equipment;

    @Column(name = "ALLOCATED_BY_ID", length = 36)
    private String allocatedById;

    @Column(name = "ALLOCATED_AT")
    private LocalDateTime allocatedAt;

    @Column(name = "RETURNED_AT")
    private LocalDateTime returnedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
