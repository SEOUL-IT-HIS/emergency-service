package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 오접수·중복 접수 이벤트: 접수팀과 합의한 규격, 발행, 다시 보내기 */
class ReceptionVoidMessagingTest {

    private static Disposition voided(String id, String receptionId, LocalDateTime decidedAt) {
        Disposition d = new Disposition();
        d.setId(id);
        d.setReceptionId(receptionId);
        d.setDispositionTypeCode(EmgCodes.DISPOSITION_VOID);
        d.setDecidedById("doc-1");
        d.setDecidedAt(decidedAt);
        d.setVoidReasonTypeCode("DUPLICATE");
        d.setVoidReason("같은 환자를 두 번 접수");
        d.setKeptReceptionId("r-keep");
        return d;
    }

    @Test
    void 이벤트는_합의한_평평한_필드와_오프셋_포함_시각으로_만든다() {
        Disposition d = voided("d-1", "r-1", LocalDateTime.of(2026, 10, 8, 14, 30, 5, 123_000_000));

        ReceptionVoidedEvent event = ReceptionVoidedEvent.of(d, "p-1");

        assertThat(event.eventType()).isEqualTo("ReceptionVoided");
        assertThat(event.version()).isEqualTo("1.0");
        assertThat(event.source()).isEqualTo("ER");
        assertThat(event.occurredAt()).isEqualTo("2026-10-08T14:30:05+09:00");   // 초 단위, 한국 시간 오프셋
        assertThat(event.receptionId()).isEqualTo("r-1");
        assertThat(event.patientId()).isEqualTo("p-1");
        assertThat(event.reasonType()).isEqualTo("DUPLICATE");
        assertThat(event.reasonText()).isEqualTo("같은 환자를 두 번 접수");
        assertThat(event.keptReceptionId()).isEqualTo("r-keep");
        assertThat(event.decidedBy()).isEqualTo("doc-1");
        assertThat(event.eventId()).hasSize(36);
        // 재전송할 때는 eventId 만 달라지고 occurredAt 은 처음 결정 시각 그대로다
        assertThat(ReceptionVoidedEvent.of(d, "p-1").eventId()).isNotEqualTo(event.eventId());
        assertThat(ReceptionVoidedEvent.of(d, "p-1").occurredAt()).isEqualTo(event.occurredAt());
    }

    @Test
    @SuppressWarnings("unchecked")
    void 발행은_접수ID를_key로_하고_JSON_필드명은_합의한_이름이다() throws Exception {
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        ReceptionIntakeRepository receptions = mock(ReceptionIntakeRepository.class);
        ReceptionIntake intake = new ReceptionIntake();
        intake.setId("r-1");
        intake.setPatientId("p-1");
        when(receptions.findById("r-1")).thenReturn(Optional.of(intake));
        KafkaReceptionVoidPublisher publisher = new KafkaReceptionVoidPublisher(kafka, new ObjectMapper(), receptions);
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "topic", "emergency.reception.voided.v1");

        publisher.publishVoided(voided("d-1", "r-1", LocalDateTime.of(2026, 10, 8, 14, 30)));

        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("emergency.reception.voided.v1"), eq("r-1"), value.capture());
        JsonNode json = new ObjectMapper().readTree(value.getValue());
        assertThat(json.fieldNames()).toIterable().containsExactlyInAnyOrder("eventId", "eventType", "version", "source",
                "occurredAt", "receptionId", "patientId", "reasonType", "reasonText", "keptReceptionId", "decidedBy");
        assertThat(json.get("patientId").asText()).isEqualTo("p-1");   // 접수에서 조회해 채운다
        assertThat(json.get("reasonType").asText()).isEqualTo("DUPLICATE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void 발행이_실패해도_예외를_던지지_않는다() {
        KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("broker down"));
        ReceptionIntakeRepository receptions = mock(ReceptionIntakeRepository.class);
        KafkaReceptionVoidPublisher publisher = new KafkaReceptionVoidPublisher(kafka, new ObjectMapper(), receptions);
        org.springframework.test.util.ReflectionTestUtils.setField(publisher, "topic", "t");

        publisher.publishVoided(voided("d-1", "r-1", LocalDateTime.now()));   // 던지면 이 줄에서 시험이 실패한다
    }

    // ---------------------------------------------------------------- 다시 보내기

    private DispositionRepository dispositions;
    private ReceptionIntakeRepository receptions;
    private ReceptionVoidPublisher publisher;
    private ReceptionVoidResender resender;

    @BeforeEach
    void setUp() {
        dispositions = mock(DispositionRepository.class);
        receptions = mock(ReceptionIntakeRepository.class);
        publisher = mock(ReceptionVoidPublisher.class);
        resender = new ReceptionVoidResender(dispositions, receptions, publisher, 72, 2);
    }

    private ReceptionIntake intake(String id, boolean cancelled) {
        ReceptionIntake r = new ReceptionIntake();
        r.setId(id);
        r.setCancelledAt(cancelled ? LocalDateTime.now() : null);
        when(receptions.findById(id)).thenReturn(Optional.of(r));
        return r;
    }

    private Disposition pending(String id, String receptionId, int minutesAgo) {
        Disposition d = voided(id, receptionId, LocalDateTime.now().minusMinutes(minutesAgo));
        when(dispositions.findByReceptionIdOrderByDecidedAtDesc(receptionId)).thenReturn(List.of(d));
        return d;
    }

    @Test
    void 접수가_아직_취소하지_않은_오접수_건만_다시_보낸다() {
        Disposition open = pending("d-open", "r-open", 30);
        intake("r-open", false);
        Disposition done = pending("d-done", "r-done", 30);
        intake("r-done", true);                       // 접수가 이미 취소함 → 다시 보내지 않는다
        Disposition fresh = pending("d-fresh", "r-fresh", 0);
        intake("r-fresh", false);                     // 방금 닫음 → 처음 발행이 도착할 시간을 준다
        when(dispositions.findByDispositionTypeCodeAndDecidedAtAfter(eq("06"), any()))
                .thenReturn(List.of(open, done, fresh));

        int sent = resender.resendPending();

        assertThat(sent).isEqualTo(1);
        verify(publisher).publishVoided(open);
        verify(publisher, never()).publishVoided(done);
        verify(publisher, never()).publishVoided(fresh);
    }

    @Test
    void 이후에_다른_결정으로_바뀌었거나_응급에_없는_접수는_보내지_않는다() {
        Disposition replaced = voided("d-old", "r-1", LocalDateTime.now().minusMinutes(30));
        Disposition newer = new Disposition();
        newer.setId("d-new");
        newer.setReceptionId("r-1");
        newer.setDispositionTypeCode(EmgCodes.DISPOSITION_HOME);
        when(dispositions.findByReceptionIdOrderByDecidedAtDesc("r-1")).thenReturn(List.of(newer, replaced));
        intake("r-1", false);
        Disposition unknown = pending("d-x", "r-x", 30);
        when(receptions.findById("r-x")).thenReturn(Optional.empty());
        when(dispositions.findByDispositionTypeCodeAndDecidedAtAfter(eq("06"), any()))
                .thenReturn(List.of(replaced, unknown));

        assertThat(resender.resendPending()).isZero();
        verify(publisher, times(0)).publishVoided(any());
    }

    @Test
    void 대상이_없으면_아무것도_보내지_않는다() {
        when(dispositions.findByDispositionTypeCodeAndDecidedAtAfter(anyString(), any())).thenReturn(List.of());
        assertThat(resender.resendPending()).isZero();
    }
}
