package kr.co.seoulit.his.emergencyservice.monitor.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.monitor.dto.*;
import kr.co.seoulit.his.emergencyservice.monitor.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-MONITOR 응급모니터링", description = "종합 현황판, 장기체류(LOS) 알림, 외부병원 정보 (Provider=EMG)")
@RestController
@RequestMapping("/api/emergency/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @Operation(summary = "응급실 종합 현황판", description = "UC-MON-01 · 대시보드")
    @GetMapping("/dashboard")
    public ApiResponse<DashboardDto> getDashboard() {
        return ApiResponse.success(monitorService.getDashboard());
    }

    @Operation(summary = "장기체류 환자 알림", description = "UC-MON-02 · LOS 임계 초과 목록")
    @GetMapping("/long-stay-alerts")
    public ApiResponse<List<LosAlertDto>> getLongStayAlerts(
            @RequestParam(required = false) Integer thresholdHours) {
        return ApiResponse.success(monitorService.getLongStayAlerts(thresholdHours));
    }

    @Operation(summary = "외부 병원 가용 정보 조회", description = "UC-MON-03 · 전원 가능 병원 (연계:NEDIS)")
    @GetMapping("/external-hospitals")
    public ApiResponse<List<ExternalHospitalDto>> getExternalHospitals() {
        return ApiResponse.success(monitorService.getExternalHospitals());
    }
}
