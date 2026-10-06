package kr.co.seoulit.his.emergencyservice.common.session;

import kr.co.seoulit.his.common.session.SessionUser;
import kr.co.seoulit.his.emergencyservice.common.config.EmergencyExceptionHandler;
import kr.co.seoulit.his.emergencyservice.common.exception.UnauthenticatedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 응급 API 로그인 검사: 켜졌을 때만 막고, 서버끼리 부르는 API와 CORS 사전 요청은 통과 */
class AuthInterceptorTest {

    private final LoginUserResolver resolver = new LoginUserResolver();

    private static MockHttpSession loggedInSession() {
        SessionUser user = new SessionUser();
        user.setEmpId("EMP-1");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", user);
        return session;
    }

    private boolean pass(boolean required, String method, String uri, boolean loggedIn) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        if (loggedIn) {
            request.setSession(loggedInSession());
        }
        return new AuthInterceptor(resolver, required).preHandle(request, new MockHttpServletResponse(), new Object());
    }

    @Test
    void whenNotRequiredNothingIsBlocked() {
        assertThat(pass(false, "GET", "/api/emergency/care/records", false)).isTrue();
    }

    @Test
    void whenRequiredARequestWithoutLoginIsRejected() {
        assertThatThrownBy(() -> pass(true, "GET", "/api/emergency/care/records", false))
                .isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> pass(true, "POST", "/api/emergency/orders", false))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void whenRequiredALoggedInUserGoesThrough() {
        assertThat(pass(true, "GET", "/api/emergency/care/records", true)).isTrue();
    }

    @Test
    void corsPreflightGoesThroughWithoutCookies() {
        assertThat(pass(true, "OPTIONS", "/api/emergency/care/records", false)).isTrue();
    }

    @Test
    void theReceptionServiceCanCallItsApisWithoutLogin() {
        // 접수 서비스는 서버끼리 부르므로 쿠키(세션)가 없다
        assertThat(pass(true, "GET", "/api/emergency/care/patients/active", false)).isTrue();
        assertThat(pass(true, "GET", "/api/emergency/care/patients/active/", false)).isTrue();
        assertThat(pass(true, "POST", "/api/emergency/care/reception-intakes", false)).isTrue();
        assertThat(pass(true, "GET", "/api/emergency/care/reception-intakes/cancellable", false)).isTrue();
    }

    @Test
    void onlyThoseApisAreExempt() {
        assertThatThrownBy(() -> pass(true, "GET", "/api/emergency/care/patients", false))
                .isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> pass(true, "POST", "/api/emergency/care/patients/active", false))
                .as("메서드가 다르면 예외가 아니다").isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> pass(true, "GET", "/api/emergency/care/reception-intakes", false))
                .isInstanceOf(UnauthenticatedException.class);
        assertThatCode(() -> pass(true, "GET", "/api/emergency/care/patients/active", true)).doesNotThrowAnyException();
    }

    @RestController
    static class PingController {
        @GetMapping("/api/emergency/ping")
        String ping() {
            return "pong";
        }
    }

    @Test
    void anUnauthenticatedRequestBecomes401WithTheCommonErrorBody() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PingController())
                .setControllerAdvice(new EmergencyExceptionHandler())
                .addInterceptors(new AuthInterceptor(resolver, true))
                .build();

        mvc.perform(get("/api/emergency/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("EMG_UNAUTHENTICATED"));

        mvc.perform(get("/api/emergency/ping").session(loggedInSession()))
                .andExpect(status().isOk());
    }
}
