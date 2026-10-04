package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 응급 처방 등록 요청(검사·약품). 처방 원장은 처방코어(OPD)가 소유하고 응급은 호출만 한다.
 * encounterId 는 다른 응급 API 와 같이 접수ID(receptionId)다. patientId 는 접수에서 서버가 채운다.
 */
@Getter
@Setter
public class OrderCreateRequestDto {
    /** 접수ID(receptionId) */
    private String encounterId;
    /** 처방의사 ID */
    private String prescribedBy;
    /** ORDER_PRIORITY_CD (STAT = 01) */
    private String priorityCode;
    /** ORDER_TIMING_CD (01 Scheduled / 02 PRN / 03 Once) */
    private String timingCode;
    /** 구두처방 여부 Y/N (기본 N). 처방코어가 verbalYn 을 지원하기 전에는 전달되지 않고 일반 처방으로 등록된다. */
    private String verbalYn;
    /** true 면 등록 직후 검사 항목은 dispatch-lab, 약품 항목은 dispatch-pharmacy 까지 호출한다(기본 false). */
    private boolean dispatchNow;
    private List<OrderItemDto> items;
}
