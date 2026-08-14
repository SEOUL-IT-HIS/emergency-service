package kr.co.seoulit.his.emergencyservice.commoncode.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * admin-service 공통 응답 래퍼 역직렬화용 ({code, message, data}).
 * kr.co.seoulit.his.emergencyservice.common.ApiResponse 와 형태는 같지만,
 * 그건 "우리가 응답 만들 때" 쓰는 클래스(private 생성자)라 외부 응답 파싱에는
 * 별도로 이 클래스를 둔다.
 *
 * code 는 admin 프론트 실제 타입(features/commonCode/types/*.ts 의 ApiResponse)을
 * 보고 number 로 맞춤 — EMG 자체 ApiResponse(문자열 코드)와 다르니 혼동 주의.
 */
@Getter
@Setter
public class AdminApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
}
