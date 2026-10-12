package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 오접수·중복 접수 이벤트를 주기적으로 다시 보낸다. Kafka 발행이 켜져 있을 때만 돈다(app.kafka.void.enabled=true).
 * 끄려면 app.kafka.void.resend.enabled=false, 간격은 app.kafka.void.resend.interval-ms (기본 10분).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.void.enabled", havingValue = "true")
public class ReceptionVoidResendScheduler {

    private final ReceptionVoidResender resender;

    @Value("${app.kafka.void.resend.enabled:true}")
    private boolean enabled;

    @Scheduled(initialDelayString = "${app.kafka.void.resend.initial-delay-ms:120000}",
            fixedDelayString = "${app.kafka.void.resend.interval-ms:600000}")
    public void resendPending() {
        if (!enabled) {
            return;
        }
        try {
            int sent = resender.resendPending();
            if (sent > 0) {
                log.info("[VOID] 접수가 아직 취소하지 않은 오접수 이벤트 {}건을 다시 보냄", sent);
            }
        } catch (Exception e) {
            log.error("[VOID] 오접수 이벤트 재전송 중 예외", e);
        }
    }
}
