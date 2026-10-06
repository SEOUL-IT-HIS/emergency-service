package kr.co.seoulit.his.emergencyservice.care.messaging;

import kr.co.seoulit.his.emergencyservice.care.messaging.dto.ReceptionIntakeEvent;
import kr.co.seoulit.his.emergencyservice.care.service.CareService;
import kr.co.seoulit.his.emergencyservice.care.service.ReceptionCancellationService;
import org.apache.kafka.common.serialization.Deserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ReceptionIntakeEventTest {

    /** application.yml 의 컨슈머 설정과 같은 역직렬화기로 JSON 을 읽는다 */
    private ReceptionIntakeEvent read(String json) {
        Map<String, Object> config = new HashMap<>();
        config.put("spring.deserializer.value.delegate.class", "org.springframework.kafka.support.serializer.JsonDeserializer");
        config.put("spring.json.trusted.packages", "kr.co.seoulit.his.emergencyservice.care.messaging.dto");
        config.put("spring.json.value.default.type", ReceptionIntakeEvent.class.getName());
        config.put("spring.json.use.type.headers", false);
        try (Deserializer<Object> d = new ErrorHandlingDeserializer<>()) {
            d.configure(config, false);
            Object value = d.deserialize("Emergency-patient-daily-list", json.getBytes(StandardCharsets.UTF_8));
            assertNotNull(value, "역직렬화 실패");
            return (ReceptionIntakeEvent) value;
        }
    }

    private static final String BASE = "\"eventId\":\"e1\",\"receptionId\":\"r1\",\"patientId\":\"p1\",\"arrivalPath\":\"01\","
            + "\"receivedAt\":\"2026-10-05T10:00:00\",\"memo\":\"m\",\"chiefComplaintRaw\":\"c\"";

    @Test
    void 등록_이벤트는_eventType이_없고_등록으로_본다() {
        ReceptionIntakeEvent e = read("{" + BASE + "}");
        assertFalse(e.isCancellation());
        assertEquals("r1", e.getReceptionId());
    }

    @Test
    void 취소_이벤트는_eventType으로_구분한다() {
        ReceptionIntakeEvent e = read("{" + BASE + ",\"eventType\":\"ReceptionCancelled\",\"status\":\"CANCELLED\"}");
        assertTrue(e.isCancellation());
        assertEquals("CANCELLED", e.getStatus());
    }

    @Test
    void 접수가_보내는_KTAS_숫자와_분류시각을_읽는다() {
        // RCP 는 ktasLevel 을 숫자(1~5)로, triageDateTime 을 날짜 문자열로 보낸다
        ReceptionIntakeEvent e = read("{" + BASE + ",\"ktasLevel\":2,\"triageDateTime\":\"2026-10-05T09:03:00\"}");
        assertEquals("2", e.getKtasLevel());
        assertEquals(java.time.LocalDateTime.of(2026, 10, 5, 9, 3), e.getTriageDateTime());
    }

    @Test
    void 취소_이벤트에_같은_KTAS_값이_있어도_읽는다() {
        ReceptionIntakeEvent e = read("{" + BASE + ",\"ktasLevel\":3,\"triageDateTime\":\"2026-10-05T09:03:00\","
                + "\"eventType\":\"ReceptionCancelled\",\"status\":\"CANCELLED\"}");
        assertTrue(e.isCancellation());
        assertEquals("3", e.getKtasLevel());
    }

    @Test
    void 처음_보는_필드가_있어도_읽는다() {
        ReceptionIntakeEvent e = read("{" + BASE + ",\"someNewField\":\"x\",\"nested\":{\"a\":1}}");
        assertEquals("r1", e.getReceptionId());
    }

    @Test
    void 취소_이벤트는_신규_접수로_저장하지_않는다() {
        CareService careService = mock(CareService.class);
        ReceptionCancellationService cancellation = mock(ReceptionCancellationService.class);
        ReceptionIntakeEventService service = new ReceptionIntakeEventService(careService, cancellation);
        service.handle(read("{" + BASE + ",\"eventType\":\"ReceptionCancelled\",\"status\":\"CANCELLED\"}"));
        verifyNoInteractions(careService);
        verify(cancellation).cancel(eq("r1"), any());
    }

    @Test
    void 등록_이벤트는_접수로_저장한다() {
        CareService careService = mock(CareService.class);
        ReceptionCancellationService cancellation = mock(ReceptionCancellationService.class);
        ReceptionIntakeEventService service = new ReceptionIntakeEventService(careService, cancellation);
        service.handle(read("{" + BASE + "}"));
        verify(careService).createReceptionIntake(any());
        verifyNoInteractions(cancellation);
    }
}
