package kr.co.seoulit.his.emergencyservice.order.service;

import kr.co.seoulit.his.emergencyservice.order.dto.MedicationDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCancelRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDispatchDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderDto;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderVerbalConfirmRequestDto;

import java.util.List;

/** 응급 처방(검사·약품) — 처방 원장은 처방코어(OPD)이고 응급은 호출만 한다(영상 오더는 처방코어가 받지 않아 제외). */
public interface OrderService {
    OrderDto createOrder(OrderCreateRequestDto request);

    OrderDto getOrder(String orderId);

    /** 약품 검색(처방 등록 때 약품 선택용) — 약품명 일부(name)가 필요하다. 결과는 최대 30건 */
    List<MedicationDto> searchMedications(String name);

    /** 구두처방 사후 확정 — 구두처방(verbalYn=Y)만, 이미 확정한 처방은 처방코어가 거절한다 */
    OrderDto confirmVerbalOrder(String orderId, OrderVerbalConfirmRequestDto request);

    /** 접수(receptionId)의 처방 목록 — 최근 처방이 먼저. items 는 비어 있다(단건 조회로 확인) */
    List<OrderDto> listOrders(String encounterId);

    /** 처방코어에는 수정 API 가 없다 — 변경은 취소 후 재등록 */
    OrderDto cancelOrder(String orderId, OrderCancelRequestDto request);

    OrderDispatchDto dispatchLab(String orderId);

    OrderDispatchDto dispatchPharmacy(String orderId);
}
