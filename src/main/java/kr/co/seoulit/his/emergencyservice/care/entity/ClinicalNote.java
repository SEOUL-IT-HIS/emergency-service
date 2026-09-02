package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "CLINICAL_NOTE")
@Getter
@Setter
public class ClinicalNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CLINICAL_NOTE_ID")
    private Long id;

    @Column(name = "RECEPTION_NO", length = 20)
    private String receptionNo;

    @Column(name = "RECORDED_BY_ID", length = 36)
    private String recordedById;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "RECORDED_AT")
    private LocalDateTime recordedAt;

    @Column(name = "SIGNED_AT")
    private LocalDateTime signedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
