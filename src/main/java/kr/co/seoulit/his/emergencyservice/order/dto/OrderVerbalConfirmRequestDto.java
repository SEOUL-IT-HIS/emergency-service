package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

/** 구두처방 사후 확정 요청 — 확정하는 의사 ID */
@Getter
@Setter
public class OrderVerbalConfirmRequestDto {
    private String confirmedBy;
}
