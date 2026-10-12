package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Kafka 연동이 꺼져 있을 때: 오접수·중복 접수는 저장만 하고 접수 서비스에 알리지 않는다. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.void.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingReceptionVoidPublisher implements ReceptionVoidPublisher {

    @Override
    public void publishVoided(Disposition disposition) {
        log.info("오접수·중복 접수 저장(Kafka 발행 꺼짐: app.kafka.void.enabled=false) receptionId={}", disposition.getReceptionId());
    }
}
