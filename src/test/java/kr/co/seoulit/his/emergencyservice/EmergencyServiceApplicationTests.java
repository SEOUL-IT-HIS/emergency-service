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
    }

    @Test
    void contextLoads() {
    }
}
