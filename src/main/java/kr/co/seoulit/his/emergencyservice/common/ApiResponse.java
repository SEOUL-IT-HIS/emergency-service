package kr.co.seoulit.his.emergencyservice.common;

import lombok.Getter;

/**
 * 공통 API 응답 래퍼 — 모든 Controller에서 ApiResponse.success(data) 반환
 * 프론트: { code, message, data }
 */
@Getter
public class ApiResponse<T> {

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "OK", data);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
