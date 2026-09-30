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
        verify(dispositionService).updateAdmissionStatus("d-1", null, EmgCodes.ADMISSION_BED_ASSIGNED, "06");
    }

    @Test
    void rejectedReplyMarksTheRequestAsRejected() {
        handler.handle("{\"dispositionId\":\"d-2\",\"rejectReason\":\"no bed\"}", false);
        verify(dispositionService).updateAdmissionStatus("d-2", null, EmgCodes.ADMISSION_REJECTED, null);
    }

    @Test
    void replyCarriesTheRequestIdWhenTheWardEchoesIt() {
        handler.handle("{\"dispositionId\":\"d-3\",\"admissionRequestId\":\"ar-9\",\"wardCode\":\"03\"}", true);
        verify(dispositionService).updateAdmissionStatus("d-3", "ar-9", EmgCodes.ADMISSION_BED_ASSIGNED, "03");
    }

    @Test
    void malformedOrIncompleteMessagesAreIgnoredWithoutThrowing() {
        handler.handle("not json", true);
        handler.handle("{\"wardCode\":\"06\"}", true);
        verify(dispositionService, never()).updateAdmissionStatus(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void anUnknownDispositionDoesNotStopTheListener() {
        when(dispositionService.updateAdmissionStatus("d-x", null, EmgCodes.ADMISSION_BED_ASSIGNED, null))
                .thenThrow(new RuntimeException("not found"));
        handler.handle("{\"dispositionId\":\"d-x\"}", true);
    }
}
