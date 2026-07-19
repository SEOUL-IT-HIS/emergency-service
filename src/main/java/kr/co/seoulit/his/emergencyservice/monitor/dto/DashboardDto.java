package kr.co.seoulit.his.emergencyservice.monitor.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class DashboardDto {
    private long totalPatients;
    private long occupiedBeds;
    private long emptyBeds;
    private long openLosAlerts;
    private List<LosAlertDto> recentLosAlerts;
}
