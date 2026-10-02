package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * 처방코어(OPD) 공통 응답 래퍼 역직렬화용 ({code, message, data}).
 * code 는 숫자일 수도 문자열일 수도 있어(예: 오류 "OPD999") String 으로 받는다 —
 * Integer 로 받으면 형식이 다를 때 역직렬화가 깨진다. 성공 여부는 HTTP 상태로 판단한다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCoreResponse<T> {
    private String code;
    private String message;
    private T data;
}
