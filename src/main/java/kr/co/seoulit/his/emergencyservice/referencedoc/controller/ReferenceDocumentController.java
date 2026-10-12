package kr.co.seoulit.his.emergencyservice.referencedoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
import kr.co.seoulit.his.emergencyservice.referencedoc.dto.ReferenceDocumentDto;
import kr.co.seoulit.his.emergencyservice.referencedoc.service.ReferenceDocumentFile;
import kr.co.seoulit.his.emergencyservice.referencedoc.service.ReferenceDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "ER-REF 응급 참고 문서", description = "응급 직원이 찾아 보는 프로토콜·지침·서식 파일 라이브러리(환자와 무관). 본문은 SeaweedFS, 목록은 응급 DB")
@RestController
@RequestMapping("/api/emergency/reference-documents")
@RequiredArgsConstructor
public class ReferenceDocumentController {

    private final ReferenceDocumentService referenceDocumentService;
    // 올린 사람·삭제한 사람은 화면에서 고른 직원이고, 비어 있으면 로그인한 사용자로 채운다
    private final LoginUserResolver loginUser;

    @Operation(summary = "참고 문서 목록 조회", description = "삭제되지 않은 문서(올린 시각 최신순). keyword 는 제목·분류·파일 이름에서 찾는다")
    @GetMapping
    public ApiResponse<List<ReferenceDocumentDto>> getDocuments(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(referenceDocumentService.getDocuments(keyword));
    }

    @Operation(summary = "참고 문서 올리기",
            description = "multipart/form-data: file(필수, 10MB 이하, pdf·이미지·Office·hwp 등), title(없으면 파일 이름), category(선택, 50자), uploadedById(없으면 로그인 사용자)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReferenceDocumentDto> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String uploadedById) throws IOException {
        return ApiResponse.success(referenceDocumentService.upload(
                file.getOriginalFilename(), file.getBytes(), title, category, loginUser.chosenOrLogin(uploadedById)));
    }

    @Operation(summary = "참고 문서 파일 받기", description = "파일 본문. pdf·이미지는 브라우저에서 바로 열고 나머지는 내려받는다")
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        ReferenceDocumentFile file = referenceDocumentService.getFile(id);
        ContentDisposition disposition = (file.inline() ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // 형식을 추측해 스크립트로 실행하지 못하게 한다
                .header("X-Content-Type-Options", "nosniff")
                .body(file.content());
    }

    @Operation(summary = "참고 문서 삭제", description = "목록에서 빼고 저장소의 파일 본문도 지운다(상태 변경 — 행은 남는다). deletedById 가 없으면 로그인 사용자")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id, @RequestParam(required = false) String deletedById) {
        referenceDocumentService.delete(id, loginUser.chosenOrLogin(deletedById));
        return ApiResponse.success(null);
    }
}
