package kr.co.seoulit.his.emergencyservice.common.config;

import kr.co.seoulit.his.emergencyservice.common.session.AuthInterceptor;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 응급 API 로그인 검사 등록.
 * application.yml: app.auth.required (환경변수 AUTH_REQUIRED, 기본 false)
 */
@Configuration
public class AuthConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AuthConfig.class);

    private final LoginUserResolver loginUserResolver;
    private final boolean authRequired;

    public AuthConfig(LoginUserResolver loginUserResolver, @Value("${app.auth.required:false}") boolean authRequired) {
        this.loginUserResolver = loginUserResolver;
        this.authRequired = authRequired;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("응급 API 로그인 검사: {} (app.auth.required={})", authRequired ? "켜짐" : "꺼짐", authRequired);
        registry.addInterceptor(new AuthInterceptor(loginUserResolver, authRequired)).addPathPatterns("/api/emergency/**");
    }
}
