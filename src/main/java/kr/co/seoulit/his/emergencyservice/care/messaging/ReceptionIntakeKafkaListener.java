package kr.co.seoulit.his.emergencyservice.care.messaging;

import kr.co.seoulit.his.emergencyservice.care.messaging.dto.ReceptionIntakeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name="app.kafka.intake.enabled", havingValue = "true")
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceptionIntakeKafkaListener {

    private final ReceptionIntakeEventService receptionIntakeEventService;

    // RCP -> EMG 응급접수 이벤트. 기존 POST /care/reception-intakes와 동일한 스키마/검증/upsert 로직을 재사용한다.
    @KafkaListener(topics = "Emergency-patient-daily-list", groupId = "emergency-service")

    public void onReceptionIntake(ReceptionIntakeEvent event) {  // 받는 타입을 ReceptionIntakeEvent로
        receptionIntakeEventService.handle(event);
        // 검증-변환-로깅은 전부 EventService 책임
    }
}
