package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReceptionVoidTopicCheckTest {

    private static final String TOPIC = "emergency.reception.voided.v1";

    private KafkaAdmin admin;
    private ReceptionVoidTopicCheck check;

    @BeforeEach
    void setUp() {
        admin = mock(KafkaAdmin.class);
        when(admin.getConfigurationProperties()).thenReturn(Map.of("bootstrap.servers", "192.168.1.126:9092"));
        check = new ReceptionVoidTopicCheck(admin, TOPIC);
    }

    @Test
    void 토픽이_있으면_브로커와_파티션을_알려준다() {
        TopicDescription description = new TopicDescription(TOPIC, false, List.of(new TopicPartitionInfo(0, null, List.of(), List.of())));
        when(admin.describeTopics(TOPIC)).thenReturn(Map.of(TOPIC, description));

        assertThat(check.check()).contains(TOPIC).contains("192.168.1.126:9092").contains("파티션=1");
    }

    @Test
    void 브로커에_토픽이_없으면_없다고_알려준다() {
        when(admin.describeTopics(TOPIC))
                .thenThrow(new KafkaException("Failed to obtain topic descriptions", new UnknownTopicOrPartitionException("no topic")));

        assertThat(check.check()).contains("브로커에 없습니다").contains(TOPIC).contains("192.168.1.126:9092");
    }

    @Test
    void 브로커에_붙지_못하면_주소를_확인하라고_알려주고_예외를_던지지_않는다() {
        when(admin.describeTopics(TOPIC)).thenThrow(new KafkaException("fail", new TimeoutException("Timed out waiting for a node assignment")));

        String message = check.check();

        assertThat(message).contains("확인하지 못했습니다").contains("KAFKA_BOOTSTRAP_SERVERS").contains("TimeoutException");
    }
}
