package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Kafka 연동이 꺼져 있을 때(기본값): 입원요청은 저장만 하고 발행하지 않는다. 병동팀과 규격 합의 후 켠다. */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.admission.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingAdmissionEventPublisher implements AdmissionEventPublisher {

    @Override
    public void publishRequested(Disposition disposition, AdmissionRequest request, String wardPref, String note) {
        log.info("입원요청 저장(Kafka 발행 꺼짐: app.kafka.admission.enabled=false) dispositionId={}", disposition.getId());
    }
}
