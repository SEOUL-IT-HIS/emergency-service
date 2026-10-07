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
    /** 처방방법 이름(예: Electronic / Verbal) */
    private String orderMethodName;
    /** 구두처방 여부 Y/N, 확정 일시·확정 의사(구두처방을 확정한 뒤에만) */
    private String verbalYn;
    private String verbalConfirmedAt;
    private String verbalConfirmedBy;
    private String prescribedBy;
    private String prescribedAt;
    private String cancelledAt;
    private String cancelReason;
    private List<OrderItemDto> items;

    /** 처방코어가 알려주는 전송 상태(목록·조회 응답): 검사는 항목 요약, 약제는 처방 단위. PENDING / SENT / FAILED, 검사 항목이 없으면 labSendStatus 는 null */
    private String labSendStatus;
    private String pharmacySendStatus;

    /**
     * 약제 조제 상태(약제에서 조회한 값, 응급 DB에 저장하지 않음). 약제 전송이 SENT 인 처방을 조회·목록으로 읽을 때만 채우고,
     * 약제에 처방이 아직 없거나 약제에 연결하지 못하면 null.
     * pharmacyStatus: RECEIVED / DISPENSED / REJECTED / CANCELLED, pharmacyReleaseStatus: RELEASED / CANCELLED / null,
     * pharmacyCancelOutcome: APPLIED(약제에 반영됨) / REFUSED(불출 이후라 미반영) / null(취소 통보 없음)
     */
    private String pharmacyStatus;
    private String pharmacyReleaseStatus;
    private String pharmacyCancelOutcome;

    /** 등록 때 dispatchNow=true 인 경우만: 전송 직후 실제 상태 SENT / FAILED / PENDING / REQUESTED(상태를 못 읽음) / NOT_APPLICABLE(해당 항목 없음). 요청하지 않았으면 null */
    private String labDispatchStatus;
    private String pharmacyDispatchStatus;
}
