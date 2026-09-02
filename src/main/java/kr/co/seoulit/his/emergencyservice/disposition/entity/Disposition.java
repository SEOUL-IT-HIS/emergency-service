package kr.co.seoulit.his.emergencyservice.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "DISPOSITION")
@Getter
@Setter
public class Disposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DISPOSITION_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "DISPOSITION_TYPE_CODE", length = 20)
    private String dispositionTypeCode;

    @Column(name = "DECIDED_BY_ID", length = 36)
    private String decidedById;

    @Column(name = "DECIDED_AT")
    private LocalDateTime decidedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
