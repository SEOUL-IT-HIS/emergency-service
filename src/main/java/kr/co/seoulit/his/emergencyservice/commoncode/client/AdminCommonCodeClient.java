package kr.co.seoulit.his.emergencyservice.commoncode.client;

import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminApiResponse;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * admin-service 공통코드 API 호출.
 * GET /api/admin/commonCodes/groups/{groupCode}
 */
@Component
@RequiredArgsConstructor
public class AdminCommonCodeClient {

    private final RestTemplate restTemplate;

    @Value("${app.admin.base-url}")
    private String adminBaseUrl;

    public List<AdminCommonCodeDto> getCommonCodes(String groupCode) {
        String url = adminBaseUrl + "/api/admin/commonCodes/groups/" + groupCode;

        ResponseEntity<AdminApiResponse<List<AdminCommonCodeDto>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        AdminApiResponse<List<AdminCommonCodeDto>> body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }
}
