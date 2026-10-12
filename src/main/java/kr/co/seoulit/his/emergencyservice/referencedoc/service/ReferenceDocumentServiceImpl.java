package kr.co.seoulit.his.emergencyservice.referencedoc.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.referencedoc.dto.ReferenceDocumentDto;
import kr.co.seoulit.his.emergencyservice.referencedoc.entity.ReferenceDocument;
import kr.co.seoulit.his.emergencyservice.referencedoc.repository.ReferenceDocumentRepository;
import kr.co.seoulit.his.emergencyservice.referencedoc.storage.ReferenceDocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 응급 참고 문서 라이브러리 — 환자와 무관하게 직원이 찾아 보는 프로토콜·지침·서식 파일.
 * 파일 본문은 SeaweedFS 버킷에, 목록(제목·분류·올린 사람)은 응급 DB(REFERENCE_DOCUMENT)에 둔다.
 * 응급이 소유하는 자료라 다른 서비스를 부르지 않는다(처방·검사·약제 결과를 저장하는 곳이 아니다).
 */
@Service
public class ReferenceDocumentServiceImpl implements ReferenceDocumentService {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDocumentServiceImpl.class);

    static final String NOT_DELETED = "N";
    static final String DELETED = "Y";

    /**
     * 올릴 수 있는 파일 형식 — 확장자로 판단하고, 응답 형식도 확장자에서 정한다(요청이 보낸 Content-Type 은 믿지 않는다).
     * 화면에서 스크립트가 실행될 수 있는 형식(html·svg 등)과 실행 파일은 받지 않는다.
     */
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", "text/plain"),
            Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("hwp", "application/x-hwp"),
            Map.entry("hwpx", "application/vnd.hancom.hwpx"));

    /** 브라우저 안에서 바로 열어 볼 형식(나머지는 내려받게 한다) */
    private static final Set<String> INLINE_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp");

    static final int TITLE_MAX = 200;
    static final int CATEGORY_MAX = 50;
    static final int FILE_NAME_MAX = 255;

    private final ReferenceDocumentRepository repository;
    private final ReferenceDocumentStorage storage;
    private final long maxFileBytes;

    public ReferenceDocumentServiceImpl(
            ReferenceDocumentRepository repository,
            ReferenceDocumentStorage storage,
            @Value("${app.reference-doc.max-file-size-mb:10}") long maxFileSizeMb) {
        this.repository = repository;
        this.storage = storage;
        this.maxFileBytes = maxFileSizeMb * 1024 * 1024;
    }

    @Override
    public List<ReferenceDocumentDto> getDocuments(String keyword) {
        String needle = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        return repository.findByDeletedYnOrderByUploadedAtDesc(NOT_DELETED).stream()
                .filter(doc -> needle == null
                        || contains(doc.getTitle(), needle)
                        || contains(doc.getCategory(), needle)
                        || contains(doc.getFileName(), needle))
                .map(this::toDto)
                .toList();
    }

    @Override
    public ReferenceDocumentDto upload(String fileName, byte[] content, String title, String category, String uploadedById) {
        if (!StringUtils.hasText(uploadedById)) {
            throw new IllegalArgumentException("uploadedById is required");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("file is required");
        }
        if (content.length > maxFileBytes) {
            throw new IllegalArgumentException("file must be at most " + (maxFileBytes / 1024 / 1024) + " MB");
        }

        String name = baseName(fileName);
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("file name is required");
        }
        if (name.length() > FILE_NAME_MAX) {
            throw new IllegalArgumentException("file name must be at most " + FILE_NAME_MAX + " characters");
        }
        String extension = extensionOf(name);
        String contentType = CONTENT_TYPES.get(extension);
        if (contentType == null) {
            throw new IllegalArgumentException("file type is not allowed (allowed: " + String.join(", ", allowedExtensions()) + ")");
        }

        String cleanTitle = StringUtils.hasText(title) ? title.trim() : name.substring(0, name.length() - extension.length() - 1);
        if (!StringUtils.hasText(cleanTitle)) {
            cleanTitle = name;
        }
        if (cleanTitle.length() > TITLE_MAX) {
            throw new IllegalArgumentException("title must be at most " + TITLE_MAX + " characters");
        }
        String cleanCategory = StringUtils.hasText(category) ? category.trim() : null;
        if (cleanCategory != null && cleanCategory.length() > CATEGORY_MAX) {
            throw new IllegalArgumentException("category must be at most " + CATEGORY_MAX + " characters");
        }

        LocalDateTime now = LocalDateTime.now();
        // 객체 키는 서버가 만든다 — 사용자가 올린 이름은 키에 쓰지 않는다(한글·공백·경로 문자 문제를 피하고 덮어쓰기를 막는다)
        String key = "reference/%04d%02d/%s.%s".formatted(now.getYear(), now.getMonthValue(), UUID.randomUUID(), extension);
        storage.put(key, content, contentType);

        ReferenceDocument entity = new ReferenceDocument();
        entity.setTitle(cleanTitle);
        entity.setCategory(cleanCategory);
        entity.setFileName(name);
        entity.setContentType(contentType);
        entity.setFileSize((long) content.length);
        entity.setObjectKey(key);
        entity.setUploadedById(uploadedById.trim());
        entity.setUploadedAt(now);
        entity.setDeletedYn(NOT_DELETED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        try {
            return toDto(repository.save(entity));
        } catch (RuntimeException exception) {
            // 목록에 등록하지 못했으면 올린 파일은 아무도 찾을 수 없다 — 치워 본다(치우지 못해도 원래 오류를 우선한다)
            try {
                storage.delete(key);
            } catch (ExternalServiceException cleanupFailure) {
                log.warn("등록에 실패한 참고 문서 파일을 치우지 못했습니다: {}", key);
            }
            throw exception;
        }
    }

    @Override
    public ReferenceDocumentFile getFile(String id) {
        ReferenceDocument doc = find(id);
        byte[] content = storage.get(doc.getObjectKey());
        return new ReferenceDocumentFile(doc.getFileName(), doc.getContentType(), content,
                INLINE_EXTENSIONS.contains(extensionOf(doc.getFileName())));
    }

    @Override
    public void delete(String id, String deletedById) {
        if (!StringUtils.hasText(deletedById)) {
            throw new IllegalArgumentException("deletedById is required");
        }
        ReferenceDocument doc = find(id);
        LocalDateTime now = LocalDateTime.now();
        doc.setDeletedYn(DELETED);
        doc.setDeletedById(deletedById.trim());
        doc.setDeletedAt(now);
        doc.setUpdatedAt(now);
        repository.save(doc);
        // 목록에서 빠진 뒤에도 저장소에 파일이 남으면 주소만 알아도 열린다 — 본문은 지운다(행은 누가·언제 지웠는지 남기려고 둔다).
        // 파일 삭제가 실패해도 삭제는 이미 끝났으므로 오류로 돌려주지 않고 남은 파일을 로그로만 알린다(저장소 화면에서 지울 수 있다).
        try {
            storage.delete(doc.getObjectKey());
        } catch (RuntimeException exception) {
            log.warn("삭제한 참고 문서의 파일을 저장소에서 지우지 못했습니다(직접 지워야 합니다): key={}, 원인={}",
                    doc.getObjectKey(), exception.getMessage());
        }
    }

    private ReferenceDocument find(String id) {
        return repository.findByIdAndDeletedYn(id, NOT_DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("reference document not found: " + id));
    }

    private ReferenceDocumentDto toDto(ReferenceDocument doc) {
        ReferenceDocumentDto dto = new ReferenceDocumentDto();
        dto.setId(doc.getId());
        dto.setTitle(doc.getTitle());
        dto.setCategory(doc.getCategory());
        dto.setFileName(doc.getFileName());
        dto.setContentType(doc.getContentType());
        dto.setFileSize(doc.getFileSize());
        dto.setUploadedById(doc.getUploadedById());
        dto.setUploadedAt(doc.getUploadedAt());
        return dto;
    }

    private static boolean contains(String value, String lowerNeedle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }

    /** 경로 구분자 앞부분을 버린다(일부 브라우저는 전체 경로를 보낸다) */
    static String baseName(String fileName) {
        if (fileName == null) {
            return null;
        }
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name;
    }

    /** 소문자 확장자(없으면 빈 문자열) */
    static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static List<String> allowedExtensions() {
        return CONTENT_TYPES.keySet().stream().sorted().toList();
    }
}
