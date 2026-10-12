package kr.co.seoulit.his.emergencyservice.referencedoc.storage;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 서명 계산이 맞는지 AWS 문서의 공개 예제(Signature Calculations for the Authorization Header —
 * "Example: GET Object")로 확인한다. 예제의 키·날짜·서명은 문서에 공개된 값이다.
 */
class AwsV4SignerTest {

    private static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    void matchesTheAwsDocumentedGetObjectExample() {
        AwsV4Signer signer = new AwsV4Signer("AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "us-east-1", "s3");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "examplebucket.s3.amazonaws.com");
        headers.put("range", "bytes=0-9");
        headers.put("x-amz-content-sha256", EMPTY_SHA256);
        headers.put("x-amz-date", "20130524T000000Z");

        String authorization = signer.authorization("GET", "/test.txt", "", headers, EMPTY_SHA256, "20130524T000000Z");

        assertThat(authorization).isEqualTo(
                "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request, "
                        + "SignedHeaders=host;range;x-amz-content-sha256;x-amz-date, "
                        + "Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41");
    }

    @Test
    void emptyPayloadHashIsTheKnownSha256OfNothing() {
        assertThat(AwsV4Signer.sha256Hex(new byte[0])).isEqualTo(EMPTY_SHA256);
    }
}
