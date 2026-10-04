package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

/** 처방 취소 요청. 처방코어는 수정 API가 없어서 변경은 취소 후 재등록으로 처리한다. */
@Getter
@Setter
public class OrderCancelRequestDto {
    private String cancelReason;
    /** 취소하는 사용자 ID */
    private String userId;
}
