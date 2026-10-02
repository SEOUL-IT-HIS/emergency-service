package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

/** 검사·약제 전송 결과. 실패하면 502 로 응답하므로 이 DTO 는 전송 성공(SENT)일 때만 내려간다. */
@Getter
@Setter
public class OrderDispatchDto {
    private String orderId;
    /** LAB / PHARMACY */
    private String target;
    private String status;
}
