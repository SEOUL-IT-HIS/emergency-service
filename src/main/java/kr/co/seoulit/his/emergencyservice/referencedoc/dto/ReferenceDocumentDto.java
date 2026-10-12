package kr.co.seoulit.his.emergencyservice.referencedoc.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 참고 문서 목록 한 건(파일 본문·저장소 키는 내려주지 않는다 — 본문은 GET /{id}/file 로 받는다) */
@Getter
@Setter
public class ReferenceDocumentDto {
    private String id;
    private String title;
    private String category;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String uploadedById;
    private LocalDateTime uploadedAt;
}
