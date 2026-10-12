package kr.co.seoulit.his.emergencyservice.referencedoc.storage;

import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SeaweedFS S3 게이트웨이(path-style: {endpoint}/{bucket}/{key})에 참고 문서 본문을 올리고 읽는다.
 *
 * 접속 정보는 환경변수로만 받는다(코드·저장소에 두지 않는다).
 *  - 서버 안에서는 게이트웨이가 localhost:8333 이다(프론트의 /files 프록시도 같은 곳을 본다).
 *  - 접근 키(SEAWEED_ACCESS_KEY / SEAWEED_SECRET_KEY)를 비워 두면 서명 없이 보낸다 — SeaweedFS S3 인증을 켜지 않은 환경용이다.
 *    인증을 켜 둔 환경이면 키를 넣어야 하고, 쓰기 권한이 있는 키여야 한다.
 *  - 버킷이 없으면 첫 업로드 때 만들어 본다(키에 버킷 생성 권한이 없으면 서버 관리자가 미리 만들어 둔다).
 *
 * 요청 하나는 연결 3초, 전체 30초 안에 끝나야 한다. 저장소가 응답하지 않으면 502 로 알린다.
 */
@Component
public class S3ReferenceDocumentStorage implements ReferenceDocumentStorage {

    private static final Logger log = LoggerFactory.getLogger(S3ReferenceDocumentStorage.class);

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final String UNAVAILABLE = "Document storage is not responding. Please try again later.";

    private final String endpoint;
    private final String bucket;
    private final AwsV4Signer signer;
    private final HttpClient client;

    public S3ReferenceDocumentStorage(
            @Value("${app.reference-doc.storage.endpoint:http://localhost:8333}") String endpoint,
            @Value("${app.reference-doc.storage.bucket:emergency-docs}") String bucket,
            @Value("${app.reference-doc.storage.region:us-east-1}") String region,
            @Value("${app.reference-doc.storage.access-key:}") String accessKey,
            @Value("${app.reference-doc.storage.secret-key:}") String secretKey) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        this.bucket = bucket;
        this.signer = StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)
                ? new AwsV4Signer(accessKey, secretKey, region, "s3")
                : null;
        // 평문 HTTP 에서 HTTP/2 업그레이드를 시도하지 않게 1.1 로 고정한다
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public void put(String key, byte[] bytes, String contentType) {
        HttpResponse<byte[]> response = send("PUT", objectPath(key), bytes, contentType);
        if (response.statusCode() == 404) {
            // 버킷이 아직 없다 — 만들고 한 번만 다시 올린다
            createBucket();
            response = send("PUT", objectPath(key), bytes, contentType);
        }
        requireSuccess(response, "upload");
    }

    @Override
    public byte[] get(String key) {
        HttpResponse<byte[]> response = send("GET", objectPath(key), null, null);
        if (response.statusCode() == 404) {
            throw new ResourceNotFoundException("reference document file not found in storage");
        }
        requireSuccess(response, "download");
        return response.body();
    }

    @Override
    public void delete(String key) {
        HttpResponse<byte[]> response = send("DELETE", objectPath(key), null, null);
        if (response.statusCode() != 404) {
            requireSuccess(response, "delete");
        }
    }

    private void createBucket() {
        HttpResponse<byte[]> response = send("PUT", encodePath("/" + bucket), new byte[0], null);
        int status = response.statusCode();
        // 409: 이미 있다(동시에 만든 경우)
        if (status != 409) {
            requireSuccess(response, "create bucket");
        }
        log.info("참고 문서 버킷을 만들었습니다: {}", bucket);
    }

    private String objectPath(String key) {
        return encodePath("/" + bucket + "/" + key);
    }

    private HttpResponse<byte[]> send(String method, String path, byte[] body, String contentType) {
        URI uri = URI.create(endpoint + path);
        byte[] payload = body != null ? body : new byte[0];
        String payloadHash = AwsV4Signer.sha256Hex(payload);
        String amzDate = AMZ_DATE.format(Instant.now());
        String host = uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();

        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("x-amz-content-sha256", payloadHash)
                .header("x-amz-date", amzDate);
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (signer != null) {
            Map<String, String> signed = new LinkedHashMap<>();
            signed.put("host", host);
            signed.put("x-amz-content-sha256", payloadHash);
            signed.put("x-amz-date", amzDate);
            request.header("Authorization", signer.authorization(method, uri.getRawPath(), "", signed, payloadHash, amzDate));
        }
        request.method(method, body != null ? HttpRequest.BodyPublishers.ofByteArray(payload) : HttpRequest.BodyPublishers.noBody());

        try {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException exception) {
            log.warn("참고 문서 저장소 호출 실패 {} {}: {}", method, path, exception.toString());
            throw new ExternalServiceException(UNAVAILABLE, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException(UNAVAILABLE, exception);
        }
    }

    private void requireSuccess(HttpResponse<byte[]> response, String action) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        // S3 오류 본문(XML)에 코드가 들어 있다 — 키·서명은 응답에 없으므로 앞부분을 남겨도 안전하다
        String detail = response.body() == null ? "" : new String(response.body(), StandardCharsets.UTF_8);
        log.warn("참고 문서 저장소가 {} 을(를) 거부했습니다: HTTP {} {}", action, status,
                detail.length() > 300 ? detail.substring(0, 300) : detail);
        throw new ExternalServiceException("Document storage rejected the request (HTTP " + status + ").");
    }

    /** 경로 조각마다 RFC 3986 로 인코딩한다('/' 는 구분자로 둔다). S3 는 경로를 한 번만 인코딩한다 */
    static String encodePath(String path) {
        StringBuilder out = new StringBuilder();
        for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xff);
            boolean unreserved = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~' || c == '/';
            if (unreserved) {
                out.append(c);
            } else {
                out.append('%').append(String.format("%02X", b & 0xff));
            }
        }
        return out.toString();
    }
}
