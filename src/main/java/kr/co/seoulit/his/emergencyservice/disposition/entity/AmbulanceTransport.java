package kr.co.seoulit.his.emergencyservice.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "AMBULANCE_TRANSPORT")
@Getter
@Setter
public class AmbulanceTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AMBULANCE_TRANSPORT_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "AMBULANCE_NO", length = 20)
    private String ambulanceNo;

    @Column(name = "TRANSPORT_TYPE_CODE", length = 20)
    private String transportTypeCode;

    @Column(name = "DEPARTED_AT")
    private LocalDateTime departedAt;

    @Column(name = "ARRIVED_AT")
    private LocalDateTime arrivedAt;

    @Column(name = "ACCOMPANYING_STAFF_ID", length = 36)
    private String accompanyingStaffId;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
