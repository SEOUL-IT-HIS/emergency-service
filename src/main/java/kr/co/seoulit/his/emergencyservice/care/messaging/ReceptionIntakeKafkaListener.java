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
    // 컨슈머 그룹과 시작 위치는 설정으로 바꿀 수 있다(기본은 공용 서버와 같은 그룹, 처음부터).
    // 개발 PC에서 같은 브로커를 보며 시험할 때는 PC 전용 그룹(app.kafka.intake.group-id)과 latest 로 두면 공용 서버와 메시지를 나눠 받지 않는다.
    @KafkaListener(topics = "Emergency-patient-daily-list",
            groupId = "${app.kafka.intake.group-id:emergency-service}",
            properties = "auto.offset.reset=${app.kafka.intake.auto-offset-reset:earliest}")

    public void onReceptionIntake(ReceptionIntakeEvent event) {  // 받는 타입을 ReceptionIntakeEvent로
        receptionIntakeEventService.handle(event);
        // 검증-변환-로깅은 전부 EventService 책임
    }
}
