package kr.co.seoulit.his.emergencyservice.referencedoc.storage;

import com.sun.net.httpserver.HttpServer;
import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** S3 호환 저장소 호출의 요청 모양(경로·헤더·서명)과 오류 처리를 가짜 S3 서버(JDK HttpServer)로 확인한다 */
class S3ReferenceDocumentStorageTest {

    private record Seen(String method, String path, String authorization, String contentType, String contentSha256, byte[] body) {
    }

    private HttpServer server;
    private final List<Seen> seen = new CopyOnWriteArrayList<>();
    /** 객체 경로 → 본문 */
    private final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    private final List<String> buckets = new CopyOnWriteArrayList<>();
    /** true 인 동안 객체 PUT 은 404(NoSuchBucket)로 답한다. 버킷 PUT 이 오면 풀린다 */
    private volatile boolean bucketMissing;
    private volatile int forcedStatus;
    private final AtomicInteger bucketCreates = new AtomicInteger();

    @BeforeEach
    void startFakeS3() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = exchange.getRequestBody().readAllBytes();
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getRawPath();
            seen.add(new Seen(method, path, exchange.getRequestHeaders().getFirst("Authorization"),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    exchange.getRequestHeaders().getFirst("x-amz-content-sha256"), body));

            int status = 200;
            byte[] response = new byte[0];
            boolean isBucketPath = path.chars().filter(c -> c == '/').count() == 1;
            if (forcedStatus != 0) {
                status = forcedStatus;
                response = "<Error><Code>AccessDenied</Code></Error>".getBytes(StandardCharsets.UTF_8);
            } else if (method.equals("PUT") && isBucketPath) {
                bucketCreates.incrementAndGet();
                bucketMissing = false;
                buckets.add(path);
            } else if (method.equals("PUT")) {
                if (bucketMissing) {
                    status = 404;
                    response = "<Error><Code>NoSuchBucket</Code></Error>".getBytes(StandardCharsets.UTF_8);
                } else {
                    objects.put(path, body);
                }
            } else if (method.equals("GET")) {
                byte[] stored = objects.get(path);
                if (stored == null) {
                    status = 404;
                } else {
                    response = stored;
                }
            } else if (method.equals("DELETE")) {
                objects.remove(path);
                status = 204;
            }
            boolean hasBody = status != 204 && response.length > 0;
            exchange.sendResponseHeaders(status, hasBody ? response.length : -1);
            if (hasBody) {
                exchange.getResponseBody().write(response);
            }
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopFakeS3() {
        server.stop(0);
    }

    private S3ReferenceDocumentStorage storage(String accessKey, String secretKey) {
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
        return new S3ReferenceDocumentStorage(endpoint, "emergency-docs", "us-east-1", accessKey, secretKey);
    }

    @Test
    void putsTheObjectUnderBucketAndKeyWithContentType() {
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        storage("", "").put("reference/202610/abc.pdf", content, "application/pdf");

        assertThat(seen).hasSize(1);
        Seen request = seen.get(0);
        assertThat(request.method()).isEqualTo("PUT");
        assertThat(request.path()).isEqualTo("/emergency-docs/reference/202610/abc.pdf");
        assertThat(request.contentType()).isEqualTo("application/pdf");
        assertThat(request.body()).isEqualTo(content);
        // 키를 안 넣으면 서명하지 않는다
        assertThat(request.authorization()).isNull();
        assertThat(request.contentSha256()).isEqualTo(AwsV4Signer.sha256Hex(content));
    }

    @Test
    void signsTheRequestWhenKeysAreConfigured() {
        storage("AKTEST", "secret-value").put("reference/202610/abc.pdf", new byte[]{1, 2, 3}, "application/pdf");

        String authorization = seen.get(0).authorization();
        assertThat(authorization).startsWith("AWS4-HMAC-SHA256 Credential=AKTEST/");
        assertThat(authorization).contains("/us-east-1/s3/aws4_request");
        assertThat(authorization).contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date");
        // 비밀 키는 요청에 실리지 않는다
        assertThat(authorization).doesNotContain("secret-value");
    }

    @Test
    void createsTheBucketOnceWhenItDoesNotExistAndRetries() {
        bucketMissing = true;

        storage("", "").put("reference/202610/abc.pdf", new byte[]{9}, "application/pdf");

        assertThat(bucketCreates.get()).isEqualTo(1);
        assertThat(buckets).containsExactly("/emergency-docs");
        assertThat(objects).containsKey("/emergency-docs/reference/202610/abc.pdf");
        // PUT 객체(404) → PUT 버킷 → PUT 객체
        assertThat(seen).extracting(Seen::method).containsExactly("PUT", "PUT", "PUT");
    }

    @Test
    void readsBackWhatWasStored() {
        S3ReferenceDocumentStorage storage = storage("", "");
        storage.put("reference/202610/abc.txt", "본문".getBytes(StandardCharsets.UTF_8), "text/plain");

        assertThat(storage.get("reference/202610/abc.txt")).isEqualTo("본문".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void missingObjectIsNotFound() {
        S3ReferenceDocumentStorage storage = storage("", "");

        assertThatThrownBy(() -> storage.get("reference/202610/none.pdf")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteIsQuietWhenObjectIsAlreadyGone() {
        S3ReferenceDocumentStorage storage = storage("", "");
        storage.put("reference/202610/abc.pdf", new byte[]{1}, "application/pdf");

        storage.delete("reference/202610/abc.pdf");
        storage.delete("reference/202610/abc.pdf");

        assertThat(objects).isEmpty();
    }

    @Test
    void rejectionIsReportedAsExternalServiceErrorWithStatus() {
        forcedStatus = 403;
        S3ReferenceDocumentStorage storage = storage("AK", "SK");

        assertThatThrownBy(() -> storage.put("reference/202610/abc.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("HTTP 403");
    }

    @Test
    void unreachableStorageIsReportedAsExternalServiceError() {
        S3ReferenceDocumentStorage unreachable =
                new S3ReferenceDocumentStorage("http://127.0.0.1:1", "emergency-docs", "us-east-1", "", "");

        assertThatThrownBy(() -> unreachable.put("reference/202610/abc.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessageContaining("not responding");
    }

    @Test
    void encodesPathSegmentsButKeepsSlashes() {
        assertThat(S3ReferenceDocumentStorage.encodePath("/emergency-docs/reference/202610/a b.pdf"))
                .isEqualTo("/emergency-docs/reference/202610/a%20b.pdf");
        assertThat(S3ReferenceDocumentStorage.encodePath("/b/한글.pdf")).isEqualTo("/b/%ED%95%9C%EA%B8%80.pdf");
    }
}
