package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 약제(PHM)의 조제 상태 조회를 서버 대 서버로 호출한다(GET, 인증 없음, 게이트웨이 없이 직접).
 *  - GET /api/pharmacy/prescriptions?prescriptionId={처방ID}  →  { data: { content: [ {status, releaseStatusCd, cancelOutcome, …} ] } }
 * 조회는 참고용이다 — 조회와 취소 사이에 약사가 조제할 수 있어 조회로 취소를 막을 수는 없다(약제 안내).
 * 조회가 화면을 막지 않도록 읽기 제한을 짧게 두고, 실패하면 예외 대신 빈 값을 돌려준다(원인은 로그).
 */
@Slf4j
@Component
public class PharmacyClient {

    private static final String PRESCRIPTIONS = "/api/pharmacy/prescriptions";
    private static final String MEDICATIONS_PAGE = "/api/pharmacy/medications/page";
    /** 약품 목록은 한 번에 100건(약제가 허용하는 최대)씩, 최대 5쪽(500건)까지 이어서 받는다 */
    static final int MEDICATION_PAGE_SIZE = 100;
    static final int MEDICATION_MAX_PAGES = 5;

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    public PharmacyClient(RestTemplateBuilder builder, @Value("${app.pharmacy.base-url}") String baseUrl) {
        this(builder.connectTimeout(Duration.ofSeconds(2)).readTimeout(Duration.ofSeconds(3)).build(), baseUrl);
    }

    PharmacyClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 조회 결과와 "약제에 연결했는지"를 함께 돌려준다 — 여러 건을 연달아 조회할 때 첫 실패에서 멈추려고 쓴다 */
    /**
     * 처방의 조제 상태. 약제에 아직 처방이 없으면 status 가 null, 연결하지 못해도 status 는 null(reachable 로 구분).
     * 결과가 여러 건이면 처방ID가 같은 것을, 처방ID가 응답에 없으면 첫 건을 쓴다.
     */
    public Lookup query(String prescriptionId) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + PRESCRIPTIONS)
                .queryParam("prescriptionId", "{prescriptionId}")
                .encode()
                .buildAndExpand(prescriptionId)
                .toUri();
        try {
            ResponseEntity<PharmacyResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, null, new ParameterizedTypeReference<PharmacyResponse>() {
                    });
            List<PharmacyPrescriptionStatus> rows = response.getBody() == null || response.getBody().getData() == null
                    || response.getBody().getData().getContent() == null
                    ? List.of() : response.getBody().getData().getContent();
            PharmacyPrescriptionStatus match = rows.stream()
                    .filter(row -> row.getPrescriptionId() == null || prescriptionId.equals(row.getPrescriptionId()))
                    .findFirst().orElse(null);
            return new Lookup(true, match);
        } catch (RestClientException e) {
            log.warn("pharmacy status lookup failed: prescriptionId={}, cause={}", prescriptionId, e.getMessage());
            return new Lookup(false, null);
        }
    }

    /**
     * 약제 약품 마스터 목록(EDI 코드가 있는 약품만, 이름순). name 이 비어 있으면 전체, 있으면 이름에 포함된 약품.
     * 처방 접수는 마스터의 EDI 코드로만 되므로 코드가 없는 약품은 받지 않는다. 한글 이름은 UTF-8 로 인코딩해 보낸다.
     * 상태 조회와 달리 약제에 연결하지 못하면 예외(502)를 던진다 — 목록이 비어 보이는 것과 구분해야 하기 때문이다.
     */
    public List<PharmacyMedication> listMedications(String name) {
        List<PharmacyMedication> all = new ArrayList<>();
        for (int page = 0; page < MEDICATION_MAX_PAGES; page++) {
            URI uri = UriComponentsBuilder.fromUriString(baseUrl + MEDICATIONS_PAGE)
                    .queryParam("name", "{name}")
                    .queryParam("ediCodeOnly", true)
                    .queryParam("page", page)
                    .queryParam("size", MEDICATION_PAGE_SIZE)
                    .encode()
                    .buildAndExpand(name == null ? "" : name.trim())
                    .toUri();
            MedicationPage body;
            try {
                ResponseEntity<MedicationResponse> response = restTemplate.exchange(
                        uri, HttpMethod.GET, null, new ParameterizedTypeReference<MedicationResponse>() {
                        });
                body = response.getBody() == null ? null : response.getBody().getData();
            } catch (RestClientException e) {
                log.warn("pharmacy medication list failed: name={}, page={}, cause={}", name, page, e.getMessage());
                throw new ExternalServiceException("pharmacy is not reachable (medication list)", e);
            }
            if (body == null || body.getContent() == null || body.getContent().isEmpty()) {
                break;
            }
            all.addAll(body.getContent());
            if (!Boolean.FALSE.equals(body.getLast())) {
                break;
            }
        }
        return all;
    }

    /** reachable=false 면 약제에 연결하지 못한 것(상태를 알 수 없음), reachable=true 에 status 가 null 이면 약제에 처방이 아직 없다 */
    public record Lookup(boolean reachable, PharmacyPrescriptionStatus status) {
        public Optional<PharmacyPrescriptionStatus> found() {
            return Optional.ofNullable(status);
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MedicationResponse {
        private MedicationPage data;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class MedicationPage {
        private List<PharmacyMedication> content;
        /** 마지막 쪽이면 true */
        private Boolean last;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class PharmacyResponse {
        private Page data;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Page {
        private List<PharmacyPrescriptionStatus> content;
    }
}
