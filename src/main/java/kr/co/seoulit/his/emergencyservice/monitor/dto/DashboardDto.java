package kr.co.seoulit.his.emergencyservice.monitor.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class DashboardDto {
    /** 재실 환자 = 접수(RECEPTION_INTAKE) 중 퇴실 결정(DISPOSITION)이 아직 없는 건 */
    private long currentPatients;
    private long occupiedBeds;
    private long availableBeds;
    /** 혼잡도는 GET /resources/congestion 의 total 과 같은 값 */
    private Double congestionRate;
    private String congestionLevel;
    private long openLosAlerts;
    private List<LosAlertDto> recentLosAlerts;
}
