package kr.co.seoulit.his.emergencyservice.common.config;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 응급 도메인 예외 처리 — starter AuthExceptionHandler 패턴 준용
 * 모든 에러 응답을 ApiResponse{code, message, data=null} 형태로 통일한다.
 *
 * 400: 요청 값 오류 / 파싱 실패
 * 404: 대상 리소스 없음
 * 409: 리소스 상태 충돌
 * 500: 처리되지 않은 서버 오류 (내부 메시지 노출 금지)
 */
@RestControllerAdvice(basePackages = "kr.co.seoulit.his.emergencyservice")
public class EmergencyExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EmergencyExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return ApiResponse.error("EMG_BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ApiResponse.error("EMG_BAD_REQUEST", "요청 본문(JSON)을 해석할 수 없습니다.");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(ResourceNotFoundException exception) {
        return ApiResponse.error("EMG_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(ConflictException exception) {
        return ApiResponse.error("EMG_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleUnexpected(Exception exception) {
        // 내부 예외 메시지는 스키마·SQL 등 민감정보가 섞일 수 있어 클라이언트에 노출하지 않는다
        log.error("Unhandled exception", exception);
        return ApiResponse.error("EMG_INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
    }
}
