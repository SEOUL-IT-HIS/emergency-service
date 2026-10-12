package kr.co.seoulit.his.emergencyservice.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * 오접수·중복 접수 이벤트 토픽을 응급이 만든다(접수팀 합의: 접수는 구독만 한다). 서버가 켜질 때 없으면 만들고, 있으면 그대로 둔다.
 * 키가 접수 ID라 같은 접수의 이벤트는 순서가 유지된다. 이벤트가 드물어 파티션은 1개로 둔다.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.void.enabled", havingValue = "true")
public class ReceptionVoidTopicConfig {

    @Bean
    public NewTopic receptionVoidedTopic(@Value("${app.kafka.void.topic:emergency.reception.voided.v1}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
}
