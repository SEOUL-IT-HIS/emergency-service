package kr.co.seoulit.his.emergencyservice.disposition.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMG", name = "TRANSFER_NOTE")
@Getter
@Setter
public class TransferNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TRANSFER_NOTE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DISPOSITION_ID", nullable = false)
    private Disposition disposition;

    @Column(name = "TARGET_HOSPITAL_CODE", length = 20)
    private String targetHospitalCode;

    @Column(name = "CONTENT", length = 4000)
    private String content;

    @Column(name = "WRITTEN_BY_ID", length = 36)
    private String writtenById;

    @Column(name = "WRITTEN_AT")
    private LocalDateTime writtenAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
