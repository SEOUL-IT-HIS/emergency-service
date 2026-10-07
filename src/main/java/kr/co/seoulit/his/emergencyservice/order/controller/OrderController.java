package kr.co.seoulit.his.emergencyservice.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
import kr.co.seoulit.his.emergencyservice.order.dto.MedicationDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderVerbalConfirmRequestDto;
import kr.co.seoulit.his.emergencyservice.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-ORDER 처방 연동", description = "처방코어(OPD) 응급 처방 호출 — 검사·약품만(영상 오더 제외). 처방 원장은 처방코어(Provider=OPD), 응급은 BFF 로 호출만 한다")
@RestController
@RequestMapping("/api/emergency/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    // 취소자는 화면에서 고른 직원이고 비어 있으면 로그인한 사용자로 채운다. 처방의(prescribedBy)·구두 확정 의사(confirmedBy)는 화면에서 의사를 골라 보내므로 요청값을 그대로 쓴다.
    private final LoginUserResolver loginUser;

    @Operation(summary = "응급 처방 등록(검사·약품)",
            description = "encounterId=접수ID. patientId 는 서버가 접수에서 채운다. 구두처방은 지금은 일반 처방으로 등록만 된다. dispatchNow=true 면 등록 직후 검사/약제 전송까지 호출")
    @PostMapping
    public ApiResponse<OrderDto> createOrder(@RequestBody OrderCreateRequestDto request) {
        return ApiResponse.success(orderService.createOrder(request));
    }

    @Operation(summary = "접수의 처방 목록 조회",
            description = "encounterId=접수ID. 처방코어 GET /prescriptions?receptionId= 호출. 최근 처방 먼저, items 없는 가벼운 목록(검사/약제 전송 상태 요약 포함) — 상세는 단건 조회")
    @GetMapping
    public ApiResponse<List<OrderDto>> listOrders(@RequestParam String encounterId) {
        return ApiResponse.success(orderService.listOrders(encounterId));
    }

    @Operation(summary = "약품 목록", description = "처방 등록 때 약품(itemCode=약품 마스터 ediCode, itemName, dosageFormCd)을 고르는 용도. 약제 medications/page 호출(EDI 코드가 있는 약품만, 이름순). name(약품명 일부)은 선택 — 비우면 전체(최대 500건). 약제가 응답하지 못하면 502")
    @GetMapping("/medications")
    public ApiResponse<List<MedicationDto>> searchMedications(@RequestParam(required = false) String name) {
        return ApiResponse.success(orderService.searchMedications(name));
    }

    @Operation(summary = "처방 단건 조회", description = "처방코어 prescriptionId(= orderId) 기준")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderDto> getOrder(@PathVariable String orderId) {
        return ApiResponse.success(orderService.getOrder(orderId));
    }

    @Operation(summary = "처방 취소", description = "처방코어에는 수정 API가 없다 — 변경은 취소 후 재등록. 삭제가 아니라 상태 변경")
    @PatchMapping("/{orderId}/cancel")
    public ApiResponse<OrderDto> cancelOrder(@PathVariable String orderId, @RequestBody OrderCancelRequestDto request) {
        request.setUserId(loginUser.chosenOrLogin(request.getUserId()));
        return ApiResponse.success(orderService.cancelOrder(orderId, request));
    }

    @Operation(summary = "구두처방 사후 확정", description = "구두처방(verbalYn=Y)을 의사가 사후에 확정한다(확정 일시·확정 의사 기록). 구두처방이 아니거나 이미 확정된 처방은 처방코어가 거절한다")
    @PatchMapping("/{orderId}/verbal-confirm")
    public ApiResponse<OrderDto> confirmVerbalOrder(@PathVariable String orderId,
                                                    @RequestBody OrderVerbalConfirmRequestDto request) {
        return ApiResponse.success(orderService.confirmVerbalOrder(orderId, request));
    }

    @Operation(summary = "검사 전송", description = "등록 뒤 처방코어가 검사(LAB)로 전송하도록 호출(자동 아님). 실패 시 502")
    @PostMapping("/{orderId}/dispatch-lab")
    public ApiResponse<OrderDispatchDto> dispatchLab(@PathVariable String orderId) {
        return ApiResponse.success(orderService.dispatchLab(orderId));
    }

    @Operation(summary = "약제 전송", description = "등록 뒤 처방코어가 약제(PHM)로 전송하도록 호출(자동 아님). 실패 시 502")
    @PostMapping("/{orderId}/dispatch-pharmacy")
    public ApiResponse<OrderDispatchDto> dispatchPharmacy(@PathVariable String orderId) {
        return ApiResponse.success(orderService.dispatchPharmacy(orderId));
    }
}
