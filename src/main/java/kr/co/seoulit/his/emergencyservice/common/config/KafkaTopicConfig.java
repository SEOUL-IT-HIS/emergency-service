package kr.co.seoulit.his.emergencyservice.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@ConditionalOnProperty(name="app.kafka.intake.enabled", havingValue = "true")
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic receptionIntakeTopic() {
        return TopicBuilder.name("Emergency-patient-daily-list")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
