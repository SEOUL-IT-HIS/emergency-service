package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 오접수·중복 접수로 닫았는데 접수가 아직 취소되지 않은 건을 접수 서비스로 다시 보낸다.
 * 처음 발행이 Kafka 장애로 실패했거나 접수가 아직 처리하지 못한 경우를 위한 것으로, 별도 저장 없이 "닫혔는데 접수가 취소되지 않았다"는
 * 상태만 본다(접수가 취소하면 ReceptionCancelled 로 응급의 CANCELLED_AT 이 채워져 더는 보내지 않는다).
 * 접수는 같은 접수를 두 번 받아도 안전하게 처리하므로 다시 보내도 문제없다. 다만 접수가 끝내 취소하지 못하는 건이 계속 쌓이지 않게
 * 결정한 지 maxAgeHours(기본 72시간)가 지난 건은 다시 보내지 않는다. 방금 닫은 건은 처음 발행이 도착할 시간을 주려고 minAgeMinutes(기본 2분) 뒤부터 본다.
 * 이 클래스는 상태를 바꾸지 않는다(발행만).
 */
@Slf4j
@Component
public class ReceptionVoidResender {

    private final DispositionRepository dispositionRepository;
    private final ReceptionIntakeRepository receptionIntakeRepository;
    private final ReceptionVoidPublisher publisher;
    private final long maxAgeHours;
    private final long minAgeMinutes;

    public ReceptionVoidResender(DispositionRepository dispositionRepository,
                                 ReceptionIntakeRepository receptionIntakeRepository,
                                 ReceptionVoidPublisher publisher,
                                 @Value("${app.kafka.void.resend.max-age-hours:72}") long maxAgeHours,
                                 @Value("${app.kafka.void.resend.min-age-minutes:2}") long minAgeMinutes) {
        this.dispositionRepository = dispositionRepository;
        this.receptionIntakeRepository = receptionIntakeRepository;
        this.publisher = publisher;
        this.maxAgeHours = maxAgeHours;
        this.minAgeMinutes = minAgeMinutes;
    }

    /** @return 다시 보낸 건수 */
    public int resendPending() {
        LocalDateTime now = LocalDateTime.now();
        List<Disposition> candidates = dispositionRepository
                .findByDispositionTypeCodeAndDecidedAtAfter(EmgCodes.DISPOSITION_VOID, now.minusHours(maxAgeHours));
        int sent = 0;
        for (Disposition disposition : candidates) {
            if (disposition.getDecidedAt() == null || disposition.getDecidedAt().isAfter(now.minusMinutes(minAgeMinutes))) {
                continue;
            }
            // 그 뒤에 다른 결정으로 바뀌었으면(이론상 06 은 최종이지만) 보내지 않는다
            boolean latest = dispositionRepository.findByReceptionIdOrderByDecidedAtDesc(disposition.getReceptionId()).stream()
                    .findFirst().map(d -> d.getId().equals(disposition.getId())).orElse(false);
            if (!latest) {
                continue;
            }
            boolean stillOpen = receptionIntakeRepository.findById(disposition.getReceptionId())
                    .map(r -> !r.isCancelled()).orElse(false);
            if (!stillOpen) {
                continue;
            }
            publisher.publishVoided(disposition);
            sent++;
        }
        return sent;
    }
}
