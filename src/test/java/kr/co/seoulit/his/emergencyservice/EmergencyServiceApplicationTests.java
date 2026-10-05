package kr.co.seoulit.his.emergencyservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class EmergencyServiceApplicationTests {

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:emg;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true;INIT=CREATE SCHEMA IF NOT EXISTS EMERGENCY");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "EMERGENCY");
        // 개인 설정(application-local.yml)이 Kafka 를 켜 두면 이 테스트가 팀 공용 브로커의 컨슈머 그룹에 끼어들고 토픽을 만든다.
        // 컨텍스트가 뜨는지만 보는 테스트이므로 Kafka 연동은 끈다.
        registry.add("app.kafka.intake.enabled", () -> "false");
        registry.add("app.kafka.admission.enabled", () -> "false");
    }

    @Test
    void contextLoads() {
    }
}
