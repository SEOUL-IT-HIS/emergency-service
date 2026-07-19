package kr.co.seoulit.his.emergencyservice.care.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.service.CareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-CARE 응급진료", description = "응급환자 목록, 진료기록·처치·투약(MAR)·CPR 기록 (Provider=EMG). 처치·MAR의 orderId는 GR2 참조")
@RestController
@RequestMapping("/api/emergency/care")
@RequiredArgsConstructor
public class CareController {

    private final CareService careService;

    @Operation(summary = "응급환자 목록 조회", description = "UC-CARE-01 · 접수 유입 환자목록 + 초기 임상정보 (연계:RCP)")
    @GetMapping("/patients")
    public ApiResponse<List<EmergencyPatientDto>> getPatients(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(careService.getPatients(date, status));
    }

    @Operation(summary = "응급 진료기록 입력", description = "UC-CARE-02 · EMR 임상노트")
    @PostMapping("/records")
    public ApiResponse<ClinicalNoteDto> createRecord(@RequestBody ClinicalNoteCreateRequestDto request) {
        return ApiResponse.success(careService.createRecord(request));
    }

    @Operation(summary = "응급 처치 기록", description = "UC-CARE-03 · 처치기록(orderId=GR2 참조 권장)")
    @PostMapping("/treatments")
    public ApiResponse<TreatmentRecordDto> createTreatment(@RequestBody TreatmentCreateRequestDto request) {
        return ApiResponse.success(careService.createTreatment(request));
    }

    @Operation(summary = "약물 투여 기록(MAR)", description = "UC-CARE-04 · 투여기록. 처방자장은 GR2, orderId 필수 권장")
    @PostMapping("/medication-administrations")
    public ApiResponse<MarDto> createMar(@RequestBody MarCreateRequestDto request) {
        return ApiResponse.success(careService.createMar(request));
    }

    @Operation(summary = "CPR 타임라인 기록", description = "UC-CARE-05 · 심폐소생술 이벤트 타임라인")
    @PostMapping("/cpr-timelines")
    public ApiResponse<CprEventDto> createCprTimeline(@RequestBody CprTimelineCreateRequestDto request) {
        return ApiResponse.success(careService.createCprTimeline(request));
    }
}
