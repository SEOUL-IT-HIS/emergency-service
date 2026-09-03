package kr.co.seoulit.his.emergencyservice.care.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "CLINICAL_NOTE")
@Getter
@Setter
public class ClinicalNote {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "CLINICAL_NOTE_ID", length = 36)
    private String id;

    @Column(name = "RECEPTION_ID", length = 36)
    private String receptionId;

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
