package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 검사·약제 전송 결과. 처방코어 호출이 거절·장애로 실패하면 502 등으로 응답하고, 호출이 받아들여지면 이 DTO 가 내려간다.
 * status 는 호출 직후 처방코어에서 다시 읽은 실제 전송 상태다: SENT / FAILED / PENDING, 읽지 못했으면 REQUESTED.
 * (처방코어가 200 을 주고도 검사 쪽으로 못 넘겨 FAILED 로 남는 경우가 있어 호출 성공과 전송 성공을 구분한다.)
 */
@Getter
@Setter
public class OrderDispatchDto {
    private String orderId;
    /** LAB / PHARMACY */
    private String target;
    private String status;
}
