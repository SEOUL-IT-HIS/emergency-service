package kr.co.seoulit.his.emergencyservice.referencedoc.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * 응급 참고 문서 — 응급실 직원이 보는 프로토콜·지침·서식 파일의 목록(메타데이터).
 * 파일 본문은 DB가 아니라 SeaweedFS(S3) 버킷에 있고, 여기에는 찾아가는 키(OBJECT_KEY)만 둔다.
 * 환자·접수와 무관한 자료라 접수 ID 를 두지 않는다. 삭제는 DELETED_YN 상태 변경이다(행·파일은 남긴다).
 */
@Entity
@Table(schema = "EMERGENCY", name = "REFERENCE_DOCUMENT")
@Getter
@Setter
public class ReferenceDocument {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "REFERENCE_DOCUMENT_ID", length = 36)
    private String id;

    @Column(name = "TITLE", length = 200, nullable = false)
    private String title;

    /** 분류(자유 입력, 예: 프로토콜·서식·약품) */
    @Column(name = "CATEGORY", length = 50)
    private String category;

    /** 올릴 때의 원래 파일 이름(내려받을 때 이 이름으로 준다) */
    @Column(name = "FILE_NAME", length = 255, nullable = false)
    private String fileName;

    @Column(name = "CONTENT_TYPE", length = 100, nullable = false)
    private String contentType;

    @Column(name = "FILE_SIZE", nullable = false)
    private Long fileSize;

    /** 버킷 안의 객체 키(서버가 만든 영문·숫자 이름 — 원래 파일 이름은 쓰지 않는다) */
    @Column(name = "OBJECT_KEY", length = 300, nullable = false)
    private String objectKey;

    @Column(name = "UPLOADED_BY_ID", length = 36, nullable = false)
    private String uploadedById;

    @Column(name = "UPLOADED_AT", nullable = false)
    private LocalDateTime uploadedAt;

    /** Y 면 삭제된 문서(목록·내려받기에서 빠진다) */
    @Column(name = "DELETED_YN", length = 1, nullable = false)
    private String deletedYn;

    @Column(name = "DELETED_BY_ID", length = 36)
    private String deletedById;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
}
