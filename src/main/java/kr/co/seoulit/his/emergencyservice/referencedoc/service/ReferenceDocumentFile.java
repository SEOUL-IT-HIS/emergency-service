package kr.co.seoulit.his.emergencyservice.referencedoc.service;

/** 내려받을 참고 문서 파일 — 본문과 응답 헤더에 쓸 이름·형식·화면 표시 여부 */
public record ReferenceDocumentFile(String fileName, String contentType, byte[] content, boolean inline) {
}
