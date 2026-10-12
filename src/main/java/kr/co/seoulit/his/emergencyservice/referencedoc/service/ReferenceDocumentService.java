package kr.co.seoulit.his.emergencyservice.referencedoc.service;

import kr.co.seoulit.his.emergencyservice.referencedoc.dto.ReferenceDocumentDto;

import java.util.List;

public interface ReferenceDocumentService {

    /** 삭제되지 않은 문서 목록(올린 시각 최신순). keyword 가 있으면 제목·분류·파일 이름에 포함된 것만 */
    List<ReferenceDocumentDto> getDocuments(String keyword);

    /** 파일을 저장소에 올리고 목록에 등록한다 */
    ReferenceDocumentDto upload(String fileName, byte[] content, String title, String category, String uploadedById);

    /** 내려받을 파일(본문과 보여줄 이름·형식) */
    ReferenceDocumentFile getFile(String id);

    /** 목록에서 빼고(상태 변경 — 행은 남긴다) 저장소의 파일 본문도 지운다 */
    void delete(String id, String deletedById);
}
