package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class BedAssignmentDto {
    private String id;
    private String receptionNo;
    private String bedId;
    private String bedNo;
    private String zoneCode;
    private String assignedById;
    private LocalDateTime assignedAt;
}
