package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CongestionDto {
    private long totalBeds;
    private long occupiedBeds;
    private long emptyBeds;
    private double occupancyRate;
}
