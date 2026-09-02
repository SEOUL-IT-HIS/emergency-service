package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipmentAssignmentCreateRequestDto {
    private String encounterId;
    private String equipmentId;
    private String allocatedById;
}
