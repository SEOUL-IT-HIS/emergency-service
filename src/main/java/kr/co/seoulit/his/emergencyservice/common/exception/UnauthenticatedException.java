package kr.co.seoulit.his.emergencyservice.common.exception;

/**
 * 로그인(세션)이 필요한 API 를 로그인 없이 호출 → HTTP 401.
 * app.auth.required=true 일 때만 던져진다.
 */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
