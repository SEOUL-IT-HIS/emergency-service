package kr.co.seoulit.his.emergencyservice.triage.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import kr.co.seoulit.his.emergencyservice.triage.service.TriageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-TRIAGE 중증도분류/상태평가", description = "EMS 정보 조회, KTAS 등급, 활력징후·격리·위험 스크리닝 (Provider=EMG)")
@RestController
@RequestMapping("/api/emergency/triage")
@RequiredArgsConstructor
public class TriageController {

    private final TriageService triageService;

    @Operation(summary = "EMS 정보 조회", description = "UC-TRI-01 · 119 이송정보 조회 (연계:EMS)")
    @GetMapping("/ems-info")
    public ApiResponse<List<EmsReferralDto>> getEmsInfo(
            @RequestParam(required = false) String receptionNo) {
        return ApiResponse.success(triageService.getEmsInfo(receptionNo));
    }

    @Operation(summary = "KTAS 등급 분류", description = "UC-TRI-02 · 최초 중증도 분류")
    @PostMapping("/ktas")
    public ApiResponse<TriageAssessmentDto> createKtas(@RequestBody KtasCreateRequestDto request) {
        return ApiResponse.success(triageService.createKtas(request));
    }

    @Operation(summary = "KTAS 등급 재평가", description = "UC-TRI-03 · 중증도 갱신")
    @PutMapping("/ktas/{id}")
    public ApiResponse<TriageAssessmentDto> updateKtas(
            @PathVariable String id,
            @RequestBody KtasUpdateRequestDto request) {
        return ApiResponse.success(triageService.updateKtas(id, request));
    }

    @Operation(summary = "활력징후 평가 등록", description = "UC-TRI-04 · 시계열 활력징후 기록")
    @PostMapping("/vital-assessments")
    public ApiResponse<List<EwsRecordDto>> createVitalAssessments(
            @RequestBody VitalAssessmentCreateRequestDto request) {
        return ApiResponse.success(triageService.createVitalAssessments(request));
    }

    @Operation(summary = "감염/격리 관리", description = "UC-TRI-05 · 격리 등록 (DUR 이력은 GR2/PHM 조회)")
    @PostMapping("/infection-isolations")
    public ApiResponse<IsolationAssessmentDto> createIsolation(
            @RequestBody IsolationCreateRequestDto request) {
        return ApiResponse.success(triageService.createIsolation(request));
    }

    @Operation(summary = "위험 스크리닝", description = "UC-TRI-06 · 낙상·자살위험 등 스크리닝")
    @PostMapping("/risk-screenings")
    public ApiResponse<RiskScreeningDto> createRiskScreening(
            @RequestBody RiskScreeningCreateRequestDto request) {
        return ApiResponse.success(triageService.createRiskScreening(request));
    }
}
