package kr.co.seoulit.his.emergencyservice.commoncode.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** admin 공통코드 호출 경로 — 구경로(/api/commonCodeGroup/list …)가 아니라 신경로(/api/admin/…)를 쓴다 */
class AdminCommonCodeClientTest {

    private static final String BASE = "http://admin.test:9191";

    private MockRestServiceServer server;
    private AdminCommonCodeClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AdminCommonCodeClient(restTemplate);
        ReflectionTestUtils.setField(client, "adminBaseUrl", BASE);
    }

    @Test
    void groupsAreReadFromTheNewAdminPath() {
        server.expect(requestTo(BASE + "/api/admin/commonCodeGroup/list"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"SUCCESS\",\"data\":["
                        + "{\"groupId\":\"g-1\",\"groupCode\":\"KTAS_LEVEL_CD\",\"groupName\":\"KTAS\",\"useYn\":\"Y\"}]}",
                        MediaType.APPLICATION_JSON));

        var groups = client.getGroups();

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).getGroupCode()).isEqualTo("KTAS_LEVEL_CD");
        server.verify();
    }

    @Test
    void itemsAreReadFromTheNewAdminPathByGroupId() {
        server.expect(requestTo(BASE + "/api/admin/commonCodeItem/list?groupId=g-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"SUCCESS\",\"data\":["
                        + "{\"codeId\":\"c-1\",\"groupId\":\"g-1\",\"codeValue\":\"01\",\"codeName\":\"Level 1\",\"useYn\":\"Y\"}]}",
                        MediaType.APPLICATION_JSON));

        var items = client.getItems("g-1");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getCodeValue()).isEqualTo("01");
        server.verify();
    }
}
