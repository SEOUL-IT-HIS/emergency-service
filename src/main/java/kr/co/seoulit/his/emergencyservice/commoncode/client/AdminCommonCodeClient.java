package kr.co.seoulit.his.emergencyservice.commoncode.client;

import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminApiResponse;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeGroupDto;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * admin-service 공통코드 API 호출.
 * 경로/필드는 hisfrontend 의 실제 admin 공통코드 관리화면 코드
 * (features/commonCode/api/commonCodeGroupApi.ts, commonCodeItemApi.ts) 기준으로 맞춘 것 —
 * emergency-service 자체 문서(docs/api.md)의 /api/admin/commonCodes/groups/{groupCode} 는
 * 실제로 존재하지 않는 경로였음 (오래된 문서, 프론트 실제 연동 코드로 재확인 후 수정).
 */
@Component
@RequiredArgsConstructor
public class AdminCommonCodeClient {

    private final RestTemplate restTemplate;

    @Value("${app.admin.base-url}")
    private String adminBaseUrl;

    /** GET /api/commonCodeGroup/list — 전체 그룹 목록 */
    public List<AdminCommonCodeGroupDto> getGroups() {
        String url = adminBaseUrl + "/api/commonCodeGroup/list";

        ResponseEntity<AdminApiResponse<List<AdminCommonCodeGroupDto>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        AdminApiResponse<List<AdminCommonCodeGroupDto>> body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }

    /** GET /api/commonCodeItem/list?groupId= — 그룹별 항목 목록 */
    public List<AdminCommonCodeItemDto> getItems(Long groupId) {
        String url = UriComponentsBuilder.fromUriString(adminBaseUrl + "/api/commonCodeItem/list")
                .queryParam("groupId", groupId)
                .toUriString();

        ResponseEntity<AdminApiResponse<List<AdminCommonCodeItemDto>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                });

        AdminApiResponse<List<AdminCommonCodeItemDto>> body = response.getBody();
        return body != null && body.getData() != null ? body.getData() : List.of();
    }
}
