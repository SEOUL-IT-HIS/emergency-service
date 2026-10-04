package kr.co.seoulit.his.emergencyservice.common.exception;

/**
 * 연계 서비스(처방코어 등) 호출이 실패했을 때 → HTTP 502.
 * 상대 서버가 응답하지 않거나(타임아웃·연결 실패) 5xx 를 돌려준 경우다.
 * 요청 값이 잘못돼 상대가 4xx 로 거부한 경우는 400(IllegalArgumentException)으로 따로 다룬다.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
