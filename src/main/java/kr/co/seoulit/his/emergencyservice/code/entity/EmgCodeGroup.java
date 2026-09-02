package kr.co.seoulit.his.emergencyservice.code.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(schema = "EMERGENCY", name = "EMG_CODE_GROUP")
@Getter
@Setter
public class EmgCodeGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EMG_CODE_GROUP_ID")
    private Long id;

    @Column(name = "GROUP_CODE", length = 30, unique = true, nullable = false)
    private String groupCode;

    @Column(name = "GROUP_NAME", length = 100)
    private String groupName;

    @Column(name = "DESCRIPTION", length = 400)
    private String description;

    @Column(name = "USE_YN", length = 1)
    private String useYn;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

}
