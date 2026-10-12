package kr.co.seoulit.his.emergencyservice.referencedoc.storage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * AWS Signature Version 4 서명(S3 호환 저장소용).
 * SeaweedFS S3 게이트웨이는 인증을 켜 두면 이 서명을 요구한다. AWS SDK 를 새로 들이지 않으려고 필요한 만큼만 직접 구현했고,
 * AWS 문서의 공개 예제(GET /test.txt)로 서명값을 검증한다(AwsV4SignerTest).
 */
final class AwsV4Signer {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";

    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final String service;

    AwsV4Signer(String accessKey, String secretKey, String region, String service) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.service = service;
    }

    /**
     * Authorization 헤더 값.
     *
     * @param canonicalUri   인코딩이 끝난 요청 경로(예: /bucket/key)
     * @param canonicalQuery 정렬·인코딩이 끝난 쿼리(없으면 빈 문자열)
     * @param headers        서명할 헤더(이름은 소문자). host 와 x-amz-* 는 반드시 넣는다
     * @param payloadHash    본문의 SHA-256(16진수)
     * @param amzDate        yyyyMMdd'T'HHmmss'Z' (UTC)
     */
    String authorization(String method, String canonicalUri, String canonicalQuery, Map<String, String> headers,
                         String payloadHash, String amzDate) {
        SortedMap<String, String> sorted = new TreeMap<>();
        headers.forEach((name, value) -> sorted.put(name.toLowerCase(), value.trim().replaceAll("\\s+", " ")));

        StringBuilder canonicalHeaders = new StringBuilder();
        sorted.forEach((name, value) -> canonicalHeaders.append(name).append(':').append(value).append('\n'));
        String signedHeaders = String.join(";", sorted.keySet());

        String canonicalRequest = method + '\n' + canonicalUri + '\n' + canonicalQuery + '\n'
                + canonicalHeaders + '\n' + signedHeaders + '\n' + payloadHash;

        String date = amzDate.substring(0, 8);
        String scope = date + '/' + region + '/' + service + "/aws4_request";
        String stringToSign = ALGORITHM + '\n' + amzDate + '\n' + scope + '\n' + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

        byte[] signingKey = hmac(hmac(hmac(hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date), region), service), "aws4_request");
        String signature = HexFormat.of().formatHex(hmac(signingKey, stringToSign));

        return ALGORITHM + " Credential=" + accessKey + '/' + scope + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
