package kr.co.seoulit.his.emergencyservice;

import kr.co.seoulit.his.emergencyservice.common.config.CorsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot 진입점 — 응급관리(EMG/UD2) MSA
 * 기본 포트: 8085
 */
@SpringBootApplication
@EnableConfigurationProperties(CorsConfig.class)
public class EmergencyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergencyServiceApplication.class, args);
    }
}
