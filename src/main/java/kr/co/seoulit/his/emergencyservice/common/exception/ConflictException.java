package kr.co.seoulit.his.emergencyservice.common.exception;

/**
 * 현재 리소스 상태와 모순되는 요청 → HTTP 409.
 * 예: 이미 사용중(OCCUPIED)인 병상에 재배정, 이미 할당된 장비 재할당.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
