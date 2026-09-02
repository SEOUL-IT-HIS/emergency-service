package kr.co.seoulit.his.emergencyservice.disposition.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.disposition.dto.*;
import kr.co.seoulit.his.emergencyservice.disposition.service.DispositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ER-DISPOSITION 이송/퇴원처리", description = "이송·귀가·입원 결정, 입원요청, 전원 소견서, 구급차 이송기록 (Provider=EMG)")
@RestController
@RequestMapping("/api/emergency/dispositions")
@RequiredArgsConstructor
public class DispositionController {

    private final DispositionService dispositionService;

    @Operation(summary = "응급 이송/퇴원 결정", description = "UC-DISP-01 · 처리형태 확정")
    @PostMapping
    public ApiResponse<DispositionDto> createDisposition(@RequestBody DispositionCreateRequestDto request) {
        return ApiResponse.success(dispositionService.createDisposition(request));
    }

    @Operation(summary = "응급 입원 요청", description = "UC-DISP-02 · 병동 입원 요청 (연계:IPT)")
    @PostMapping("/{id}/admission-request")
    public ApiResponse<AdmissionRequestDto> createAdmissionRequest(
            @PathVariable String id,
            @RequestBody AdmissionRequestCreateDto request) {
        return ApiResponse.success(dispositionService.createAdmissionRequest(id, request));
    }

    @Operation(summary = "전원 소견서 작성", description = "UC-DISP-03 · 소견서(투약이력은 GET /api/orders 조회, 연계:GR2)")
    @PostMapping("/{id}/transfer-note")
    public ApiResponse<TransferNoteDto> createTransferNote(
            @PathVariable String id,
            @RequestBody TransferNoteCreateDto request) {
        return ApiResponse.success(dispositionService.createTransferNote(id, request));
    }

    @Operation(summary = "구급차 이송 기록", description = "UC-DISP-04 · 이송 메타정보 (연계:EMS)")
    @PostMapping("/{id}/ambulance-transport")
    public ApiResponse<AmbulanceTransportDto> createAmbulanceTransport(
            @PathVariable String id,
            @RequestBody AmbulanceTransportCreateDto request) {
        return ApiResponse.success(dispositionService.createAmbulanceTransport(id, request));
    }
}
