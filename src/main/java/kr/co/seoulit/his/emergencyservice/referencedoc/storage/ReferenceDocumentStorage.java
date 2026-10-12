package kr.co.seoulit.his.emergencyservice.referencedoc.storage;

/**
 * 참고 문서 파일 본문 저장소(SeaweedFS S3 버킷).
 * 실패(저장소가 응답하지 않음·거부)는 ExternalServiceException(502), 없는 객체를 읽으면 ResourceNotFoundException(404)이다.
 */
public interface ReferenceDocumentStorage {

    /** 객체를 올린다(같은 키가 있으면 덮어쓴다). 버킷이 없으면 만들어 본다 */
    void put(String key, byte[] bytes, String contentType);

    /** 객체 본문을 읽는다 */
    byte[] get(String key);

    /** 객체를 지운다(없어도 오류가 아니다). 올린 직후 DB 저장이 실패했을 때 남는 파일을 치우는 데 쓴다 */
    void delete(String key);
}
