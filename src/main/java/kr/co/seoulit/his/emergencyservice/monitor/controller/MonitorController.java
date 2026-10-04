package kr.co.seoulit.his.emergencyservice.monitor.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
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
    // 확인자는 로그인한 사용자로 기록한다
    private final LoginUserResolver loginUser;

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

    @Operation(summary = "장기체류 알림 확인 처리", description = "UC-MON-02 · 담당자가 알림을 확인(acknowledge)하면 미확인 목록에서 빠진다")
    @PatchMapping("/long-stay-alerts/{alertId}/acknowledge")
    public ApiResponse<LosAlertDto> acknowledgeLongStayAlert(
            @PathVariable String alertId,
            @RequestBody LosAlertAcknowledgeRequestDto request) {
        request.setAcknowledgedById(loginUser.actorOr(request.getAcknowledgedById()));
        return ApiResponse.success(monitorService.acknowledgeLongStayAlert(alertId, request));
    }
}
