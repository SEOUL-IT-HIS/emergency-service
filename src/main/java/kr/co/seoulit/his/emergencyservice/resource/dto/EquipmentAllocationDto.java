package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EquipmentAllocationDto {
    private Long id;
    private String receptionNo;
    private Long equipmentId;
    private String assetNo;
    private String allocatedById;
    private LocalDateTime allocatedAt;
}
