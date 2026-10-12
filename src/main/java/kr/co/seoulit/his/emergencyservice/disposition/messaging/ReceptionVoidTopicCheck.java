package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 서버가 켜진 직후, 응급이 어느 Kafka 브로커에 붙어 있고 오접수 이벤트 토픽이 거기에 있는지 로그로 남긴다.
 * 토픽 생성(ReceptionVoidTopicConfig)은 브로커에 붙지 못하면 조용히 넘어가서, 서버가 다른 브로커를 보고 있어도
 * 로그에서는 알아채기 어렵다. 접수팀이 "토픽이 안 보인다"고 할 때 가장 먼저 볼 줄이다.
 * 확인만 하고 아무것도 만들지 않으며, 실패해도 서버 기동에는 영향이 없다(별도 스레드에서 한 번만).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.void.enabled", havingValue = "true")
public class ReceptionVoidTopicCheck {

    private final KafkaAdmin kafkaAdmin;
    private final String topic;

    public ReceptionVoidTopicCheck(KafkaAdmin kafkaAdmin,
                                   @Value("${app.kafka.void.topic:emergency.reception.voided.v1}") String topic) {
        this.kafkaAdmin = kafkaAdmin;
        this.topic = topic;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkInBackground() {
        Thread thread = new Thread(this::check, "reception-void-topic-check");
        thread.setDaemon(true);
        thread.start();
    }

    /** @return 로그에 남긴 한 줄(테스트용) */
    String check() {
        String broker = String.valueOf(kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"));
        try {
            Map<String, TopicDescription> found = kafkaAdmin.describeTopics(topic);
            TopicDescription description = found.get(topic);
            if (description == null) {
                return warn("오접수 이벤트 토픽이 브로커에 없습니다: topic=%s, 브로커=%s", topic, broker);
            }
            String message = "오접수 이벤트 토픽 확인: topic=%s, 브로커=%s, 파티션=%d"
                    .formatted(topic, broker, description.partitions().size());
            log.info(message);
            return message;
        } catch (RuntimeException exception) {
            if (rootCause(exception) instanceof UnknownTopicOrPartitionException) {
                return warn("오접수 이벤트 토픽이 브로커에 없습니다: topic=%s, 브로커=%s", topic, broker);
            }
            return warn("오접수 이벤트 토픽을 확인하지 못했습니다: topic=%s, 브로커=%s (%s) — 응급 서버가 브로커에 붙어 있는지, "
                            + "주소(환경변수 KAFKA_BOOTSTRAP_SERVERS 또는 spring.kafka.bootstrap-servers)를 확인하세요",
                    topic, broker, rootMessage(exception));
        }
    }

    private String warn(String format, Object... args) {
        String message = format.formatted(args);
        log.warn(message);
        return message;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = rootCause(throwable);
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
