package kr.co.seoulit.his.emergencyservice.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 타 서비스(admin 등) REST 호출용 공통 RestTemplate.
 * 커넥션이 오래 걸리면 서버 기동/요청 처리가 지연되므로 타임아웃을 짧게 둔다.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }
}
