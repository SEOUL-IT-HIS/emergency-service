package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 응급 → 접수 오접수·중복 접수 이벤트를 Kafka 로 발행한다(app.kafka.void.enabled=true).
 * 토픽은 app.kafka.void.topic (기본 emergency.reception.voided.v1), 메시지 key 는 receptionId.
 * 서비스가 DB 커밋 뒤에 호출하고, 발행 실패가 퇴실 결정 저장을 막지 않는다. 실패하거나 접수가 취소하지 못한 건은
 * {@link ReceptionVoidResender} 가 주기적으로 다시 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.void.enabled", havingValue = "true")
public class KafkaReceptionVoidPublisher implements ReceptionVoidPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ReceptionIntakeRepository receptionIntakeRepository;

    @Value("${app.kafka.void.topic:emergency.reception.voided.v1}")
    private String topic;

    @Override
    public void publishVoided(Disposition disposition) {
        String receptionId = disposition.getReceptionId();
        String patientId = receptionIntakeRepository.findById(receptionId).map(ReceptionIntake::getPatientId).orElse(null);
        ReceptionVoidedEvent event = ReceptionVoidedEvent.of(disposition, patientId);
        try {
            kafkaTemplate.send(topic, receptionId, objectMapper.writeValueAsString(event)).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("오접수 이벤트 발행 실패(주기적으로 다시 보낸다) receptionId={} : {}", receptionId, error.getMessage());
                }
            });
            log.info("오접수 이벤트 발행 topic={} receptionId={} reasonType={}", topic, receptionId, event.reasonType());
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("오접수 이벤트 발행 실패(주기적으로 다시 보낸다) receptionId={} : {}", receptionId, e.getMessage());
        }
    }
}
