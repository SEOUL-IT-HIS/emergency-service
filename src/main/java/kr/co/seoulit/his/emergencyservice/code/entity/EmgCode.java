package kr.co.seoulit.his.emergencyservice.code.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EMG_CODE")
@Getter
@Setter
public class EmgCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMG_CODE_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMG_CODE_GROUP_ID", nullable = false)
    private EmgCodeGroup codeGroup;

    @Column(name = "CODE_VALUE", length = 30, nullable = false)
    private String codeValue;

    @Column(name = "CODE_NAME", length = 100)
    private String codeName;

    @Column(name = "DESCRIPTION", length = 400)
    private String description;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "USE_YN", length = 1)
    private String useYn;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
