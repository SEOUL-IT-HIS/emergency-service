package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BedAssignmentCreateRequestDto {
    private String encounterId;
    private Long bedId;
    private String assignedById;
}
