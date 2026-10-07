package kr.co.seoulit.his.emergencyservice.order.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** 약제(PHM) 조제 상태 조회: 주소, 페이지 응답 읽기, 실패 시 빈 값 */
class PharmacyClientTest {

    private static final String BASE = "http://phm.test:8082";
    private static final String ORDER_ID = "3f2b8c1e-7a4d-4e5b-9c61-2d8f0a1b5e77";
    private static final String URL = BASE + "/api/pharmacy/prescriptions?prescriptionId=" + ORDER_ID;

    private MockRestServiceServer server;
    private PharmacyClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new PharmacyClient(restTemplate, BASE + "/");   // 끝의 / 는 떼어진다
    }

    @Test
    void readsTheStateFromThePagedResponseAndIgnoresUnknownFields() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"ok\",\"data\":{\"content\":[{"
                        + "\"prescriptionId\":\"" + ORDER_ID + "\",\"status\":\"DISPENSED\",\"releaseStatusCd\":\"RELEASED\","
                        + "\"cancelOutcome\":\"REFUSED\",\"somethingNew\":1}],\"totalElements\":1}}", MediaType.APPLICATION_JSON));

        PharmacyClient.Lookup lookup = client.query(ORDER_ID);

        assertThat(lookup.reachable()).isTrue();
        assertThat(lookup.status().getStatus()).isEqualTo("DISPENSED");
        assertThat(lookup.status().getReleaseStatusCd()).isEqualTo("RELEASED");
        assertThat(lookup.status().getCancelOutcome()).isEqualTo("REFUSED");
        server.verify();
    }

    @Test
    void anEmptyResultMeansThePharmacyHasNoRecordYetNotAFailure() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"content\":[],\"empty\":true}}", MediaType.APPLICATION_JSON));

        PharmacyClient.Lookup lookup = client.query(ORDER_ID);

        assertThat(lookup.reachable()).isTrue();
        assertThat(lookup.status()).isNull();
        assertThat(lookup.found()).isEmpty();
    }

    @Test
    void rowsOfOtherPrescriptionsAreNotMistakenForOurs() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"data\":{\"content\":[{\"prescriptionId\":\"other\",\"status\":\"REJECTED\"}]}}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.query(ORDER_ID).status()).isNull();
    }

    @Test
    void aServerErrorOrAnUnreadableAnswerIsReportedAsUnreachableInsteadOfThrowing() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        PharmacyClient.Lookup down = client.query(ORDER_ID);
        assertThat(down.reachable()).isFalse();
        assertThat(down.status()).isNull();

        server.reset();
        server.expect(requestTo(URL)).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThat(client.query(ORDER_ID).reachable()).isFalse();
    }

    private static final String LIST_URL = BASE + "/api/pharmacy/medications/page?name=%s&ediCodeOnly=true&page=%d&size=100";

    @Test
    void medicationListSendsTheKoreanNameAsUtf8AndReadsTheMasterFields() {
        server.expect(requestTo(String.format(LIST_URL, "%ED%83%80%EC%9D%B4%EB%A0%88%EB%86%80", 0)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":200,\"data\":{\"content\":[{\"medicationId\":1,\"ediCode\":\"EDI-TYLENOL-500\","
                        + "\"medicationName\":\"타이레놀정500mg\",\"dosageFormCd\":\"01\",\"formCodeName\":\"정제\",\"entpName\":\"한국얀센\","
                        + "\"chart\":null}],\"last\":true}}", MediaType.APPLICATION_JSON));

        var found = client.listMedications(" 타이레놀 ");   // 앞뒤 공백은 뗀다

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getEdiCode()).isEqualTo("EDI-TYLENOL-500");
        assertThat(found.get(0).getDosageFormCd()).isEqualTo("01");
        server.verify();
    }

    @Test
    void medicationListFollowsThePagesUntilTheLastOne() {
        server.expect(requestTo(String.format(LIST_URL, "", 0)))
                .andRespond(withSuccess("{\"data\":{\"content\":[{\"ediCode\":\"A\",\"medicationName\":\"가\"}],\"last\":false}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(String.format(LIST_URL, "", 1)))
                .andRespond(withSuccess("{\"data\":{\"content\":[{\"ediCode\":\"B\",\"medicationName\":\"나\"}],\"last\":true}}", MediaType.APPLICATION_JSON));

        assertThat(client.listMedications(null)).extracting("ediCode").containsExactly("A", "B");
        server.verify();
    }

    @Test
    void anEmptyMedicationListIsEmptyButAnOutageIsAnError() {
        server.expect(requestTo(String.format(LIST_URL, "x", 0)))
                .andRespond(withSuccess("{\"data\":{\"content\":[],\"last\":true}}", MediaType.APPLICATION_JSON));
        assertThat(client.listMedications("x")).isEmpty();

        server.reset();
        server.expect(requestTo(String.format(LIST_URL, "x", 0))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.listMedications("x"))
                .isInstanceOf(kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException.class);
    }
}
