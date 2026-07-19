package kr.co.seoulit.his.emergencyservice.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Swagger(OpenAPI 3) 문서 메타 정보.
 * UI: http://localhost:8085/swagger-ui/index.html
 * JSON: http://localhost:8085/v3/api-docs
 *
 * 문서화 범위 = EMG(UD2)가 Provider로 직접 제공하는 API만.
 * 처방 SoT는 GR2(/api/orders*)이며, EMG는 이를 Consumer로 호출할 뿐이므로 여기 노출하지 않는다.
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.api.base-url:http://localhost:8085}")
    private String baseUrl;

    @Bean
    public OpenAPI emergencyServiceOpenAPI() {
        Info info = new Info()
                .title("EMG 응급관리 서비스 API")
                .version("v1")
                .description("""
                        응급관리(EMG/UD2) MSA가 Provider로 제공하는 REST API 문서.

                        - 문서화 대상: EMG 자체 소유 API (triage · resource · care · channel · monitor · disposition · codes)
                        - 처방/오더 Source of Truth는 GR2 처방코어(/api/orders*)이며, EMG는 Consumer로 호출한다(본 문서 제외).
                        - 알레르기 등 안전정보 SoT는 PAT. EMG는 조회만 가능(이중기입 금지).
                        - 공통 응답 래퍼: { code, message, data }
                        """)
                .contact(new Contact().name("Seoul IT HIS - Emergency").email("emergency@seoulit.co.kr"));

        Server localServer = new Server()
                .url(baseUrl)
                .description("Local/Dev");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }

    /**
     * 모든 오퍼레이션에 공통 에러 응답을 전역 등록.
     * 개별 컨트롤러에 @ApiResponse를 반복하지 않기 위한 DRY 처리.
     * EmergencyExceptionHandler가 실제로 내려주는 응답과 1:1로 맞춘다.
     *
     * 트레이드오프: 전역 일괄 등록은 간결하지만 일부 API에는 발생하지 않는 코드도
     * 함께 표기된다(예: 대시보드 조회의 404). 정밀하게 하려면 메서드 레벨
     * @ApiResponse로 재정의하면 병합된다.
     */
    @Bean
    public OperationCustomizer commonErrorResponses() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            responses.addApiResponse("400", errorResponse(
                    "잘못된 요청 — 필수 값 누락, 검증 실패, JSON 파싱 실패",
                    "EMG_BAD_REQUEST",
                    "encounterId and ktasScore are required"));
            responses.addApiResponse("404", errorResponse(
                    "대상 리소스 없음 — 존재하지 않는 id 지정",
                    "EMG_NOT_FOUND",
                    "bed not found: 999"));
            responses.addApiResponse("409", errorResponse(
                    "상태 충돌 — 이미 사용중인 병상/장비 재배정 등",
                    "EMG_CONFLICT",
                    "bed already occupied: 3"));
            responses.addApiResponse("500", errorResponse(
                    "서버 내부 오류 — 상세 내용은 서버 로그 참조(클라이언트 미노출)",
                    "EMG_INTERNAL_ERROR",
                    "서버 내부 오류가 발생했습니다."));
            return operation;
        };
    }

    private io.swagger.v3.oas.models.responses.ApiResponse errorResponse(
            String description, String code, String message) {
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("code", code);
        example.put("message", message);
        example.put("data", null);

        MediaType mediaType = new MediaType().example(example);
        return new io.swagger.v3.oas.models.responses.ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", mediaType));
    }
}
