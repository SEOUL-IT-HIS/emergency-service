package kr.co.seoulit.his.emergencyservice.common.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.seoulit.his.common.session.SessionUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 세션에서 로그인 사용자 읽기 — 속성 이름과 무관하게 SessionUser 타입으로 찾고, 읽지 못하면 "로그인 정보 없음" */
class LoginUserResolverTest {

    private final LoginUserResolver resolver = new LoginUserResolver();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static SessionUser user(String empId) {
        SessionUser user = new SessionUser();
        user.setEmpId(empId);
        user.setEmpName(empId + "-name");
        return user;
    }

    private static MockHttpServletRequest requestWithSession(String attributeName, Object value) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(attributeName, value);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        return request;
    }

    @Test
    void theSessionUserIsFoundWhateverTheAttributeIsNamed() {
        // admin 이 어떤 이름으로 저장했는지 몰라도 SessionUser 타입이면 찾는다
        for (String name : new String[] {"loginUser", "SESSION_USER", "x"}) {
            var found = resolver.from(requestWithSession(name, user("EMP-1")));
            assertThat(found).as(name).isPresent();
            assertThat(found.get().getEmpId()).isEqualTo("EMP-1");
        }
    }

    @Test
    void otherSessionAttributesAreNotALogin() {
        assertThat(resolver.from(requestWithSession("foo", "bar"))).isEmpty();
    }

    @Test
    void noSessionMeansNotLoggedInAndNoSessionIsCreated() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(resolver.from(request)).isEmpty();
        assertThat(request.getSession(false)).as("로그인 확인 때문에 세션을 새로 만들면 Redis 에 빈 세션이 쌓인다").isNull();
    }

    @Test
    void aSessionUserWithoutEmpIdIsNotALogin() {
        assertThat(resolver.from(requestWithSession("loginUser", user("")))).isEmpty();
        assertThat(resolver.from(requestWithSession("loginUser", user(null)))).isEmpty();
    }

    @Test
    void aSessionStoreFailureMeansNotLoggedInInsteadOfAnError() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenThrow(new IllegalStateException("redis down"));

        assertThat(resolver.from(request)).isEmpty();
    }

    @Test
    void theSessionIsReadOnlyOncePerRequest() {
        AtomicInteger reads = new AtomicInteger();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user("EMP-1"));
        MockHttpServletRequest loggedIn = new MockHttpServletRequest() {
            @Override
            public HttpSession getSession(boolean create) {
                reads.incrementAndGet();
                return session;
            }
        };

        resolver.from(loggedIn);
        resolver.from(loggedIn);
        assertThat(reads.get()).isEqualTo(1);

        AtomicInteger anonymousReads = new AtomicInteger();
        MockHttpServletRequest anonymous = new MockHttpServletRequest() {
            @Override
            public HttpSession getSession(boolean create) {
                anonymousReads.incrementAndGet();
                return null;
            }
        };
        resolver.from(anonymous);
        resolver.from(anonymous);
        assertThat(anonymousReads.get()).isEqualTo(1);
    }

    @Test
    void theLoggedInUserReplacesWhateverTheRequestClaims() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestWithSession("loginUser", user("EMP-ME"))));

        assertThat(resolver.actorOr("someone-else")).isEqualTo("EMP-ME");
        assertThat(resolver.actorOr("  ")).isEqualTo("EMP-ME");
        assertThat(resolver.actorOr(null)).isEqualTo("EMP-ME");
        assertThat(resolver.actorOr("EMP-ME")).isEqualTo("EMP-ME");
    }

    @Test
    void withoutALoginTheRequestValueIsKept() {
        // 로그인 정보를 읽을 수 없는 환경(단독 실행 등)
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertThat(resolver.actorOr("typed-id")).isEqualTo("typed-id");
        assertThat(resolver.actorOr(null)).isNull();
    }

    @Test
    void outsideAnHttpRequestTheRequestValueIsKept() {
        // Kafka 리스너·스케줄러 등 요청 밖
        assertThat(resolver.current()).isEmpty();
        assertThat(resolver.actorOr("SYSTEM")).isEqualTo("SYSTEM");
    }
}
