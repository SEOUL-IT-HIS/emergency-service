package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 병동 → 응급 입원요청 회신 구독(app.kafka.admission.enabled=true 일 때만).
 * 기존 접수 컨슈머(ReceptionIntakeEvent JSON)와 설정이 겹치지 않게, 이 리스너는 문자열 그대로 받는
 * 전용 컨테이너 팩토리를 쓴다. 토픽 이름은 설정으로 바꿀 수 있다.
 */
@Configuration
@ConditionalOnProperty(name = "app.kafka.admission.enabled", havingValue = "true")
public class AdmissionReplyKafkaListener {

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> admissionReplyContainerFactory(KafkaProperties properties) {
        Map<String, Object> config = properties.buildConsumerProperties(null);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "emergency-service-admission");
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        return factory;
    }

    @Component
    @RequiredArgsConstructor
    @ConditionalOnProperty(name = "app.kafka.admission.enabled", havingValue = "true")
    static class Listener {

        private final AdmissionReplyHandler handler;

        @KafkaListener(topics = "${app.kafka.admission.bed-assigned-topic:inpatient.admission.bed-assigned.v1}",
                containerFactory = "admissionReplyContainerFactory")
        public void onBedAssigned(String payload) {
            handler.handle(payload, true);
        }

        @KafkaListener(topics = "${app.kafka.admission.rejected-topic:inpatient.admission.rejected.v1}",
                containerFactory = "admissionReplyContainerFactory")
        public void onRejected(String payload) {
            handler.handle(payload, false);
        }
    }
}
