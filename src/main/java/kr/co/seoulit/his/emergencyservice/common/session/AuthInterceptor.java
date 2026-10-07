package kr.co.seoulit.his.emergencyservice.common.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.seoulit.his.emergencyservice.common.exception.UnauthenticatedException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 응급 API(/api/emergency/**) 로그인 검사.
 * required=true 일 때 로그인(세션)이 없으면 401(EMG_UNAUTHENTICATED)을 준다. required=false 면 아무것도 막지 않는다.
 */
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 다른 서비스가 서버끼리 부르는 API — 브라우저가 아니라 쿠키(세션)가 없으므로 로그인 검사에서 뺀다.
     * 접수 서비스: 중복 접수 확인(GET /care/patients/active), 접수 취소 가능 여부(GET /care/reception-intakes/cancellable), 접수 정보 REST 전송(POST /care/reception-intakes).
     */
    static final Set<String> SERVER_TO_SERVER = Set.of(
            "GET /api/emergency/care/patients/active",
            "GET /api/emergency/care/reception-intakes/cancellable",
            "POST /api/emergency/care/reception-intakes");

    private final LoginUserResolver loginUserResolver;
    private final boolean required;

    public AuthInterceptor(LoginUserResolver loginUserResolver, boolean required) {
        this.loginUserResolver = loginUserResolver;
        this.required = required;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!required || isPreflight(request) || isServerToServer(request)) {
            return true;
        }
        if (loginUserResolver.from(request).isEmpty()) {
            // 로그인이 필요합니다.
            throw new UnauthenticatedException("Login is required.");
        }
        return true;
    }

    /** CORS 사전 요청(OPTIONS)은 쿠키 없이 온다 */
    private static boolean isPreflight(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    static boolean isServerToServer(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return SERVER_TO_SERVER.contains(request.getMethod().toUpperCase() + " " + path);
    }
}
