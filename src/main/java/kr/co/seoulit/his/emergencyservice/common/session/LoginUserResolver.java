package kr.co.seoulit.his.emergencyservice.common.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.seoulit.his.common.session.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Optional;

/**
 * 요청을 보낸 로그인 사용자를 세션에서 읽는다.
 *
 * 로그인은 admin 이 처리한다. admin 이 Redis 에 세션을 저장하고 브라우저에 SESSION 쿠키를 주면,
 * 브라우저가 보낸 그 쿠키로 이 서비스(spring-session-data-redis)가 같은 세션을 읽는다.
 * 세션에서 꺼내는 것은 모든 MSA 가 공유하는 SessionUser(empId, empName …)다.
 * admin 이 SessionUser 를 어떤 속성 이름으로 저장하는지는 몰라도 되도록, 속성 이름 대신 타입(SessionUser)으로 찾는다.
 *
 * 세션을 읽지 못하면(로그인 안 함, 세션 저장소 연결 실패, 직렬화 불일치) "로그인 정보 없음"으로 본다.
 */
@Component
public class LoginUserResolver {

    private static final Logger log = LoggerFactory.getLogger(LoginUserResolver.class);

    /** 한 요청 안에서 세션 저장소(Redis)를 여러 번 읽지 않도록 결과를 요청에 붙여 둔다 */
    private static final String CACHE_ATTRIBUTE = LoginUserResolver.class.getName() + ".USER";
    private static final Object NOT_LOGGED_IN = new Object();

    /** 이 요청의 로그인 사용자 */
    public Optional<SessionUser> from(HttpServletRequest request) {
        Object cached = request.getAttribute(CACHE_ATTRIBUTE);
        if (cached instanceof SessionUser user) {
            return Optional.of(user);
        }
        if (cached == NOT_LOGGED_IN) {
            return Optional.empty();
        }
        SessionUser found = read(request);
        request.setAttribute(CACHE_ATTRIBUTE, found != null ? found : NOT_LOGGED_IN);
        return Optional.ofNullable(found);
    }

    /** 지금 처리 중인 HTTP 요청의 로그인 사용자. 요청 밖(Kafka 리스너·스케줄러)이면 비어 있다 */
    public Optional<SessionUser> current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return from(attributes.getRequest());
        }
        return Optional.empty();
    }

    /**
     * 기록할 처리자 ID. 로그인한 사용자가 있으면 그 사람의 empId 다 — 요청에 실린 값은 화면이 보낸 것이라
     * 바꿔 보낼 수 있으므로 무시한다. 로그인 정보를 읽을 수 없는 환경(단독 실행 등)에서만 요청값을 그대로 쓴다.
     */
    public String actorOr(String requested) {
        Optional<SessionUser> user = current();
        if (user.isEmpty()) {
            return requested;
        }
        String empId = user.get().getEmpId();
        if (StringUtils.hasText(requested) && !requested.trim().equals(empId)) {
            log.info("처리자 ID를 로그인 사용자로 대체 - 요청={}, 로그인={}", requested.trim(), empId);
        }
        return empId;
    }

    private SessionUser read(HttpServletRequest request) {
        try {
            // false: 새 세션을 만들지 않는다 — 만들면 로그인과 무관한 빈 세션이 Redis 에 쌓인다
            HttpSession session = request.getSession(false);
            if (session == null) {
                return null;
            }
            for (String name : Collections.list(session.getAttributeNames())) {
                if (session.getAttribute(name) instanceof SessionUser user && StringUtils.hasText(user.getEmpId())) {
                    return user;
                }
            }
        } catch (RuntimeException exception) {
            log.warn("로그인 세션을 읽지 못했습니다(세션 저장소 연결·직렬화 확인): {}", exception.toString());
        }
        return null;
    }
}
