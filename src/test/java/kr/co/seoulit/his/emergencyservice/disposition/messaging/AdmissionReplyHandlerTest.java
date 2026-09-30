package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.service.DispositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 병동 회신 → 입원요청 상태 반영 (Kafka 없이) */
class AdmissionReplyHandlerTest {

    private DispositionService dispositionService;
    private AdmissionReplyHandler handler;

    @BeforeEach
    void setUp() {
        dispositionService = mock(DispositionService.class);
        handler = new AdmissionReplyHandler(dispositionService, new ObjectMapper());
    }

    @Test
    void bedAssignedReplyMarksTheRequestAsBedAssigned() {
        handler.handle("{\"dispositionId\":\"d-1\",\"wardCode\":\"06\",\"bedId\":\"b-9\"}", true);
        verify(dispositionService).updateAdmissionStatus("d-1", EmgCodes.ADMISSION_BED_ASSIGNED);
    }

    @Test
    void rejectedReplyMarksTheRequestAsRejected() {
        handler.handle("{\"dispositionId\":\"d-2\",\"rejectReason\":\"no bed\"}", false);
        verify(dispositionService).updateAdmissionStatus("d-2", EmgCodes.ADMISSION_REJECTED);
    }

    @Test
    void malformedOrIncompleteMessagesAreIgnoredWithoutThrowing() {
        handler.handle("not json", true);
        handler.handle("{\"wardCode\":\"06\"}", true);
        verify(dispositionService, never()).updateAdmissionStatus(anyString(), anyString());
    }

    @Test
    void anUnknownDispositionDoesNotStopTheListener() {
        when(dispositionService.updateAdmissionStatus("d-x", EmgCodes.ADMISSION_BED_ASSIGNED))
                .thenThrow(new RuntimeException("not found"));
        handler.handle("{\"dispositionId\":\"d-x\"}", true);
    }
}
