package kr.co.seoulit.his.emergencyservice.resource.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.resource.dto.*;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-RESOURCE 자원관리", description = "혼잡도 조회, 병상·장비 배정 (Provider=EMG)")
@RestController
@RequestMapping("/api/emergency/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @Operation(summary = "실시간 자원 혼잡도 조회", description = "UC-RES-01 · 과밀화 지표 (연계:NEDIS)")
    @GetMapping("/congestion")
    public ApiResponse<CongestionDto> getCongestion() {
        return ApiResponse.success(resourceService.getCongestion());
    }

    @Operation(summary = "병상 목록 조회", description = "UC-RES-02 · 구역/상태별 병상 조회 (배정 화면에서 빈 병상 선택용)")
    @GetMapping("/beds")
    public ApiResponse<List<BedDto>> getBeds(
            @RequestParam(required = false) String zoneCode,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(resourceService.getBeds(zoneCode, status));
    }

    @Operation(summary = "구역-병상 배정", description = "UC-RES-02 · 침상 배당")
    @PostMapping("/bed-assignments")
    public ApiResponse<BedAssignmentDto> assignBed(@RequestBody BedAssignmentCreateRequestDto request) {
        return ApiResponse.success(resourceService.assignBed(request));
    }

    @Operation(summary = "구역-병상 배정 해제", description = "UC-RES-02 · 병상 해제 (환자 퇴실/전실 시)")
    @PatchMapping("/bed-assignments/{assignmentId}/release")
    public ApiResponse<BedAssignmentDto> releaseBed(
            @PathVariable String assignmentId,
            @RequestBody BedReleaseRequestDto request) {
        return ApiResponse.success(resourceService.releaseBed(assignmentId, request));
    }

    @Operation(summary = "응급 의료기기 할당", description = "UC-RES-03 · 기기 배당")
    @PostMapping("/equipment-assignments")
    public ApiResponse<EquipmentAllocationDto> assignEquipment(
            @RequestBody EquipmentAssignmentCreateRequestDto request) {
        return ApiResponse.success(resourceService.assignEquipment(request));
    }
}
