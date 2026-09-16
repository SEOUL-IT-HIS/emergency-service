package kr.co.seoulit.his.emergencyservice.patient.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * patient-service 공통 응답 래퍼 역직렬화용 ({code, message, data}).
 * kr.co.seoulit.his.emergencyservice.common.ApiResponse 와 형태는 같지만,
 * 그건 "우리가 응답 만들 때" 쓰는 클래스(private 생성자)라 외부 응답 파싱에는
 * 별도로 이 클래스를 둔다. commoncode.dto.AdminApiResponse와 같은 이유·같은 패턴.
 */
@Getter
@Setter
public class PatientApiResponse<T> {
    private Integer code;
    private String message;
    private T data;
}
