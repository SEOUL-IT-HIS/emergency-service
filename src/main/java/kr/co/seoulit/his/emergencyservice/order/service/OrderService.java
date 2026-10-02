package kr.co.seoulit.his.emergencyservice.order.service;

import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;

/** 응급 처방(검사·약품) — 처방 원장은 처방코어(OPD)이고 응급은 호출만 한다(영상 오더는 처방코어가 받지 않아 제외). */
public interface OrderService {
    OrderDto createOrder(OrderCreateRequestDto request);

    OrderDto getOrder(String orderId);

    /** 처방코어에는 수정 API 가 없다 — 변경은 취소 후 재등록 */
    OrderDto cancelOrder(String orderId, OrderCancelRequestDto request);

    OrderDispatchDto dispatchLab(String orderId);

    OrderDispatchDto dispatchPharmacy(String orderId);
}
