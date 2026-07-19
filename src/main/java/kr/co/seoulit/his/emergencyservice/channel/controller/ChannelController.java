package kr.co.seoulit.his.emergencyservice.channel.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.channel.dto.*;
import kr.co.seoulit.his.emergencyservice.channel.service.ChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ER-CHANNEL 채널연계", description = "협진 요청, 온콜 호출 (Provider=EMG). 오더 자장은 GR2 orders 경로로 분리")
@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @Operation(summary = "협진 요청", description = "UC-ORD-09 · 응급 협진(오더 자장 아님, 연계:OPD/IPT). channelRef 권장")
    @PostMapping("/consultations")
    public ApiResponse<ConsultRequestDto> createConsultation(@RequestBody ConsultCreateRequestDto request) {
        return ApiResponse.success(channelService.createConsultation(request));
    }

    @Operation(summary = "온콜 의사 호출", description = "UC-ORD-11 · 당직 알림(연계:ADM). encounterId, targetRole")
    @PostMapping("/on-call-pages")
    public ApiResponse<OncallRequestDto> createOnCallPage(@RequestBody OncallCreateRequestDto request) {
        return ApiResponse.success(channelService.createOnCallPage(request));
    }
}
