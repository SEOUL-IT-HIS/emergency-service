package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AmbulanceTransportCreateDto {
    private String ambulanceNo;
    private String transportTypeCode;
    private LocalDateTime departedAt;
    private LocalDateTime arrivedAt;
    private String accompanyingStaffId;
}
