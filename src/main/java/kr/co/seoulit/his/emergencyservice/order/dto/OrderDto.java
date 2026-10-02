package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 처방 조회·등록 응답. 처방 내용의 원본은 처방코어이고 응급 DB에는 저장하지 않는다(orderId만 참조).
 * 시각은 처방코어가 준 문자열 그대로 전달한다.
 */
@Getter
@Setter
public class OrderDto {
    /** 처방ID(처방코어 prescriptionId, UUID 36자) — 투약·처치 기록의 orderId 로 쓴다 */
    private String orderId;
    /** 접수ID(receptionId) */
    private String encounterId;
    private String status;
    private String orderMethod;
    private String priorityCode;
    private String timingCode;
    private String verbalYn;
    private String prescribedBy;
    private String prescribedAt;
    private String cancelledAt;
    private String cancelReason;
    private List<OrderItemDto> items;

    /** 처방코어가 알려주는 전송 상태(목록·조회 응답): 검사는 항목 요약, 약제는 처방 단위. PENDING / SENT / FAILED, 검사 항목이 없으면 labSendStatus 는 null */
    private String labSendStatus;
    private String pharmacySendStatus;

    /** 등록 때 dispatchNow=true 인 경우만: SENT / FAILED / NOT_APPLICABLE(해당 항목 없음). 요청하지 않았으면 null */
    private String labDispatchStatus;
    private String pharmacyDispatchStatus;
}
