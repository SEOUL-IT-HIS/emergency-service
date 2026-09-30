package kr.co.seoulit.his.emergencyservice.monitor.service;

import kr.co.seoulit.his.emergencyservice.monitor.dto.*;
import java.util.List;

public interface MonitorService {
    DashboardDto getDashboard();
    List<LosAlertDto> getLongStayAlerts(Integer thresholdHours);
    int detectLongStayPatients();
    LosAlertDto acknowledgeLongStayAlert(String alertId, LosAlertAcknowledgeRequestDto request);
}
