package kr.co.seoulit.his.emergencyservice.common.exception;

/**
 * 대상 리소스 없음 → HTTP 404.
 * "요청 형식은 맞지만 대상이 없음"은 400(IllegalArgumentException)과 구분한다.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " not found: " + id);
    }
}
