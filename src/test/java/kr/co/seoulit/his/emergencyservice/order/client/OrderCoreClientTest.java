package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 처방코어(OPD) 호출: 주소·본문·쿼리 인코딩과 오류 변환 */
class OrderCoreClientTest {

    private static final String BASE = "http://opd.test:8088";
    private static final String RECEPTION_ID = "0f0aa769-0f3b-4504-9f40-04044cb0d60e";
    private static final String ORDER_ID = "3f2b8c1e-7a4d-4e5b-9c61-2d8f0a1b5e77";

    private MockRestServiceServer server;
    private OrderCoreClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new OrderCoreClient(restTemplate, new ObjectMapper(), BASE + "/");   // 끝의 / 는 떼어진다
    }

    private OrderCoreCreateRequest request() {
        OrderCoreCreateRequest.Item item = new OrderCoreCreateRequest.Item();
        item.setPrescriptionType("검사");
        item.setItemCode("LAB001");
        item.setItemName("CBC");
        item.setDetailInfo("STAT");
        OrderCoreCreateRequest body = new OrderCoreCreateRequest();
        body.setPatientId("p-1");
        body.setPrescribedBy("dr-1");
        body.setDepartmentCode("10");
        body.setServiceType("ER");
        body.setOrderMethod("01");
        body.setPriorityCode("01");
        body.setTimingCode("03");
        body.setItems(List.of(item));
        return body;
    }

    @Test
    void createPostsToTheEmergencyEndpointAndReadsThePrescriptionIdEvenWhenCodeIsAString() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/emergency/" + RECEPTION_ID))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.serviceType").value("ER"))
                .andExpect(jsonPath("$.departmentCode").value("10"))
                .andExpect(jsonPath("$.patientId").value("p-1"))
                .andExpect(jsonPath("$.items[0].prescriptionType").value("검사"))
                // 쓰지 않는 값(null)은 본문에 싣지 않는다
                .andExpect(jsonPath("$.verbalYn").doesNotExist())
                .andExpect(jsonPath("$.items[0].dosage").doesNotExist())
                .andRespond(withSuccess("{\"code\":\"SUCCESS\",\"message\":\"OK\",\"data\":"
                        + "{\"prescriptionId\":\"" + ORDER_ID + "\",\"status\":\"ACTIVE\",\"unknownField\":1}}",
                        MediaType.APPLICATION_JSON));

        OrderCorePrescription created = client.create(RECEPTION_ID, request());

        assertThat(created.getPrescriptionId()).isEqualTo(ORDER_ID);
        assertThat(created.getStatus()).isEqualTo("ACTIVE");
        server.verify();
    }

    @Test
    void numericCodeInTheWrapperIsAcceptedToo() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/emergency/" + RECEPTION_ID))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"OK\",\"data\":{\"prescriptionId\":\"" + ORDER_ID + "\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.create(RECEPTION_ID, request()).getPrescriptionId()).isEqualTo(ORDER_ID);
    }

    @Test
    void aResponseWithoutAPrescriptionIdIsAnUpstreamError() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/emergency/" + RECEPTION_ID))
                .andRespond(withSuccess("{\"code\":\"SUCCESS\",\"data\":{}}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.create(RECEPTION_ID, request()))
                .isInstanceOf(ExternalServiceException.class).hasMessageContaining("prescriptionId");
    }

    @Test
    void errorsAreTranslatedByStatus() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/emergency/" + RECEPTION_ID))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"OPD400\",\"message\":\"patientId is required\"}"));
        assertThatThrownBy(() -> client.create(RECEPTION_ID, request()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("patientId is required");

        server.reset();
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/" + ORDER_ID)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> client.get(ORDER_ID)).isInstanceOf(ResourceNotFoundException.class);

        server.reset();
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/" + ORDER_ID))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"OPD999\",\"message\":\"A system error occurred.\"}"));
        assertThatThrownBy(() -> client.get(ORDER_ID))
                .isInstanceOf(ExternalServiceException.class).hasMessageContaining("A system error occurred.");
    }

    @Test
    void aMissingEmergencyEndpointOnTheOrderCoreIsAnUpstreamErrorNotAMissingPrescription() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/emergency/" + RECEPTION_ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.create(RECEPTION_ID, request()))
                .isInstanceOf(ExternalServiceException.class).hasMessageContaining("404");
    }

    @Test
    void deactivateSendsTheReasonAndUserAsEncodedQueryParameters() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/" + ORDER_ID
                        + "/deactivate?cancelReason=%EC%98%A4%EB%8D%94%20%EC%98%A4%EB%A5%98&userId=dr-1"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess());

        client.deactivate(ORDER_ID, "오더 오류", "dr-1");

        server.verify();
    }

    @Test
    void dispatchCallsPostWithoutABody() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/" + ORDER_ID + "/dispatch-lab"))
                .andExpect(method(HttpMethod.POST)).andExpect(content().string("")).andRespond(withSuccess());
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions/" + ORDER_ID + "/dispatch-pharmacy"))
                .andExpect(method(HttpMethod.POST)).andRespond(withSuccess());

        client.dispatchLab(ORDER_ID);
        client.dispatchPharmacy(ORDER_ID);

        server.verify();
    }

    @Test
    void listByReceptionCallsTheReceptionFilterAndReadsTheSendStatusSummaries() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions?receptionId=" + RECEPTION_ID))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":\"SUCCESS\",\"message\":\"OK\",\"data\":["
                        + "{\"prescriptionId\":\"" + ORDER_ID + "\",\"receptionId\":\"" + RECEPTION_ID + "\","
                        + "\"status\":\"ORDERED\",\"priorityCode\":\"01\",\"labSendStatus\":\"PENDING\","
                        + "\"pharmacySendStatus\":\"SENT\",\"somethingNew\":true},"
                        + "{\"prescriptionId\":\"p2\",\"labSendStatus\":null}]}", MediaType.APPLICATION_JSON));

        List<OrderCorePrescription> list = client.listByReception(RECEPTION_ID);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getPrescriptionId()).isEqualTo(ORDER_ID);
        assertThat(list.get(0).getLabSendStatus()).isEqualTo("PENDING");
        assertThat(list.get(0).getPharmacySendStatus()).isEqualTo("SENT");
        assertThat(list.get(0).getPriorityCode()).isEqualTo("01");
        assertThat(list.get(1).getLabSendStatus()).isNull();      // 검사 항목이 없으면 null
        server.verify();
    }

    @Test
    void anEmptyOrMissingListIsAnEmptyList() {
        server.expect(requestTo(BASE + "/api/outpatient/prescriptions?receptionId=" + RECEPTION_ID))
                .andRespond(withSuccess("{\"code\":\"SUCCESS\",\"data\":null}", MediaType.APPLICATION_JSON));

        assertThat(client.listByReception(RECEPTION_ID)).isEmpty();
    }
}
