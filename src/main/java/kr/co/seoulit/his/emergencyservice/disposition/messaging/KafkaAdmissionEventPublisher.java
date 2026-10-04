package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.triage.repository.IsolationAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;

/**
 * 응급 → 병동 입원요청을 Kafka 로 발행한다(app.kafka.admission.enabled=true).
 * 토픽 이름은 설정으로 바꿀 수 있다: app.kafka.admission.requested-topic (기본 emergency.admission.requested.v1).
 * 서비스가 DB 커밋 뒤에 호출한다. 발행 실패가 입원요청 저장을 막지 않는다(저장은 이미 끝난 뒤, 실패는 로그로 남기고 화면에서 상태가 '요청됨'으로 남는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.admission.enabled", havingValue = "true")
public class KafkaAdmissionEventPublisher implements AdmissionEventPublisher {

    /** 병동팀 합의 형식: 2026-10-01T14:30:00 (소수 초 없음) */
    static final DateTimeFormatter REQUESTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final IsolationAssessmentRepository isolationAssessmentRepository;

    @Value("${app.kafka.admission.requested-topic:emergency.admission.requested.v1}")
    private String requestedTopic;

    @Override
    public void publishRequested(Disposition disposition, AdmissionRequest request, String wardPref, String note) {
        String receptionId = disposition.getReceptionId();
        String patientId = receptionIntakeRepository.findById(receptionId)
                .map(ReceptionIntake::getPatientId).orElse(null);
        // 해제되지 않은 격리평가 중 격리가 필요(Y)한 건이 있으면 격리 필요
        boolean isolation = isolationAssessmentRepository.findByReceptionId(receptionId).stream()
                .anyMatch(a -> a.getReleasedAt() == null && "Y".equals(a.getRequiredYn()));
        AdmissionRequestedEvent event = new AdmissionRequestedEvent(
                disposition.getId(), request.getId(), receptionId, patientId, request.getTargetDeptCode(), wardPref,
                isolation ? "Y" : "N", disposition.getDecidedById(),
                request.getRequestedAt().format(REQUESTED_AT_FORMAT), StringUtils.hasText(note) ? note : null);
        try {
            kafkaTemplate.send(requestedTopic, disposition.getId(), objectMapper.writeValueAsString(event));
            log.info("입원요청 발행 topic={} dispositionId={}", requestedTopic, disposition.getId());
        } catch (JsonProcessingException | RuntimeException e) {
            log.error("입원요청 발행 실패 dispositionId={} : {}", disposition.getId(), e.getMessage());
        }
    }
}
