package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.function.Supplier;

/**
 * 처방코어(OPD, outpatient-service)를 서버 대 서버로 호출한다. 응급 처방은 전용 API 를 쓴다.
 *  - 등록   POST  /api/outpatient/prescriptions/emergency/{receptionId}
 *  - 조회   GET   /api/outpatient/prescriptions/{prescriptionId}
 *  - 취소   PATCH /api/outpatient/prescriptions/{prescriptionId}/deactivate?cancelReason=&userId=
 *  - 전송   POST  /api/outpatient/prescriptions/{prescriptionId}/dispatch-lab | dispatch-pharmacy (자동 호출 아님)
 * 처방 수정 API 는 없다(취소 후 재등록). 전용 RestTemplate 을 쓴다 — 공용 RestTemplate(3초)보다 읽기 제한을 길게 둔다.
 *
 * 오류 변환: 404 → ResourceNotFoundException(404), 그 외 4xx → IllegalArgumentException(400, 처방코어 메시지 포함),
 * 5xx·타임아웃·연결 실패 → ExternalServiceException(502).
 */
@Slf4j
@Component
public class OrderCoreClient {

    private static final String PRESCRIPTIONS = "/api/outpatient/prescriptions";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    @Autowired
    public OrderCoreClient(RestTemplateBuilder builder, ObjectMapper objectMapper,
                           @Value("${app.order.base-url}") String baseUrl) {
        this(builder.connectTimeout(Duration.ofSeconds(3)).readTimeout(Duration.ofSeconds(10)).build(),
                objectMapper, baseUrl);
    }

    OrderCoreClient(RestTemplate restTemplate, ObjectMapper objectMapper, String baseUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 응급 처방을 등록하고 처방코어가 만든 처방(prescriptionId 포함)을 돌려준다. */
    public OrderCorePrescription create(String receptionId, OrderCoreCreateRequest body) {
        String url = baseUrl + PRESCRIPTIONS + "/emergency/" + receptionId;
        OrderCorePrescription created;
        try {
            created = call("create prescription", receptionId, () -> {
                ResponseEntity<OrderCoreResponse<OrderCorePrescription>> response = restTemplate.exchange(
                        url, HttpMethod.POST, new HttpEntity<>(body),
                        new ParameterizedTypeReference<OrderCoreResponse<OrderCorePrescription>>() {
                        });
                return response.getBody() == null ? null : response.getBody().getData();
            });
        } catch (ResourceNotFoundException e) {
            // 등록에서의 404 는 "처방이 없다"가 아니라 응급 전용 API 가 아직 없는 서버(배포 전)이거나 주소가 틀린 경우다
            throw new ExternalServiceException("order core returned 404 for the emergency prescription API "
                    + "(not deployed yet or wrong base-url): " + PRESCRIPTIONS + "/emergency/{receptionId}", e);
        }
        if (created == null || created.getPrescriptionId() == null || created.getPrescriptionId().isBlank()) {
            throw new ExternalServiceException("order core did not return a prescriptionId");
        }
        return created;
    }

    public OrderCorePrescription get(String prescriptionId) {
        String url = baseUrl + PRESCRIPTIONS + "/" + prescriptionId;
        OrderCorePrescription found = call("get prescription", prescriptionId, () -> {
            ResponseEntity<OrderCoreResponse<OrderCorePrescription>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<OrderCoreResponse<OrderCorePrescription>>() {
                    });
            return response.getBody() == null ? null : response.getBody().getData();
        });
        if (found == null) {
            throw ResourceNotFoundException.of("prescription", prescriptionId);
        }
        return found;
    }

    /** 취소(비활성화). 사유·사용자 ID 는 쿼리 파라미터로 보낸다(한글은 인코딩). */
    public void deactivate(String prescriptionId, String cancelReason, String userId) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + PRESCRIPTIONS + "/{id}/deactivate")
                .queryParam("cancelReason", "{cancelReason}")
                .queryParam("userId", "{userId}")
                .encode()
                .buildAndExpand(prescriptionId, cancelReason, userId)
                .toUri();
        call("deactivate prescription", prescriptionId, () -> restTemplate.exchange(uri, HttpMethod.PATCH, null, Void.class));
    }

    public void dispatchLab(String prescriptionId) {
        post(prescriptionId, "dispatch-lab", "dispatch lab");
    }

    public void dispatchPharmacy(String prescriptionId) {
        post(prescriptionId, "dispatch-pharmacy", "dispatch pharmacy");
    }

    private void post(String prescriptionId, String action, String what) {
        String url = baseUrl + PRESCRIPTIONS + "/" + prescriptionId + "/" + action;
        call(what, prescriptionId, () -> restTemplate.exchange(url, HttpMethod.POST, null, Void.class));
    }

    private <T> T call(String what, String target, Supplier<T> action) {
        try {
            return action.get();
        } catch (HttpStatusCodeException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            String message = errorMessage(e);
            log.warn("order core {} failed: target={}, status={}, message={}", what, target, e.getStatusCode(), message);
            if (status == HttpStatus.NOT_FOUND) {
                throw ResourceNotFoundException.of("prescription", target);
            }
            if (e.getStatusCode().is4xxClientError()) {
                throw new IllegalArgumentException("order core rejected the request (" + what + "): " + message);
            }
            throw new ExternalServiceException("order core error (" + what + "): " + message, e);
        } catch (ResourceAccessException e) {
            log.warn("order core {} unreachable: target={}, cause={}", what, target, e.getMessage());
            throw new ExternalServiceException("order core is not reachable (" + what + ")", e);
        }
    }

    /** 처방코어 오류 응답의 message 를 꺼낸다. 형식이 달라 못 읽으면 HTTP 상태를 쓴다. */
    private String errorMessage(HttpStatusCodeException e) {
        try {
            String message = objectMapper.readTree(e.getResponseBodyAsString()).path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // 본문이 JSON 이 아니면 아래 상태 문구를 쓴다
        }
        return "HTTP " + e.getStatusCode().value();
    }
}
