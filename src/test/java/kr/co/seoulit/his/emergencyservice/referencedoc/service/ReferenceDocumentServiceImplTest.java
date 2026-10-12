package kr.co.seoulit.his.emergencyservice.referencedoc.service;

import kr.co.seoulit.his.emergencyservice.common.exception.ExternalServiceException;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.referencedoc.dto.ReferenceDocumentDto;
import kr.co.seoulit.his.emergencyservice.referencedoc.entity.ReferenceDocument;
import kr.co.seoulit.his.emergencyservice.referencedoc.repository.ReferenceDocumentRepository;
import kr.co.seoulit.his.emergencyservice.referencedoc.storage.ReferenceDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReferenceDocumentServiceImplTest {

    /** 메모리에 두는 가짜 저장소 */
    private static class FakeStorage implements ReferenceDocumentStorage {
        final Map<String, byte[]> objects = new HashMap<>();
        final Map<String, String> types = new HashMap<>();
        RuntimeException failOnPut;

        @Override
        public void put(String key, byte[] bytes, String contentType) {
            if (failOnPut != null) {
                throw failOnPut;
            }
            objects.put(key, bytes);
            types.put(key, contentType);
        }

        @Override
        public byte[] get(String key) {
            byte[] bytes = objects.get(key);
            if (bytes == null) {
                throw new ResourceNotFoundException("missing");
            }
            return bytes;
        }

        @Override
        public void delete(String key) {
            objects.remove(key);
        }
    }

    private ReferenceDocumentRepository repository;
    private FakeStorage storage;
    private ReferenceDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ReferenceDocumentRepository.class);
        when(repository.save(any(ReferenceDocument.class))).thenAnswer(inv -> {
            ReferenceDocument doc = inv.getArgument(0);
            if (doc.getId() == null) {
                doc.setId("doc-1");
            }
            return doc;
        });
        storage = new FakeStorage();
        service = new ReferenceDocumentServiceImpl(repository, storage, 10);
    }

    private static byte[] bytes(int n) {
        return new byte[n];
    }

    private static ReferenceDocument doc(String id, String title, String category, String fileName) {
        ReferenceDocument doc = new ReferenceDocument();
        doc.setId(id);
        doc.setTitle(title);
        doc.setCategory(category);
        doc.setFileName(fileName);
        doc.setContentType("application/pdf");
        doc.setFileSize(3L);
        doc.setObjectKey("reference/202610/" + id + ".pdf");
        doc.setUploadedById("emp-1");
        doc.setUploadedAt(LocalDateTime.now());
        doc.setDeletedYn("N");
        return doc;
    }

    // ---------------------------------------------------------------- 올리기

    @Test
    void uploadsFileAndRegistersIt() {
        ReferenceDocumentDto dto = service.upload("Sepsis Protocol.pdf", bytes(5), "  Sepsis bundle  ", " Protocol ", " emp-1 ");

        assertThat(dto.getId()).isEqualTo("doc-1");
        assertThat(dto.getTitle()).isEqualTo("Sepsis bundle");
        assertThat(dto.getCategory()).isEqualTo("Protocol");
        assertThat(dto.getFileName()).isEqualTo("Sepsis Protocol.pdf");
        assertThat(dto.getContentType()).isEqualTo("application/pdf");
        assertThat(dto.getFileSize()).isEqualTo(5L);
        assertThat(dto.getUploadedById()).isEqualTo("emp-1");
        assertThat(dto.getUploadedAt()).isNotNull();

        ArgumentCaptor<ReferenceDocument> saved = ArgumentCaptor.forClass(ReferenceDocument.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getDeletedYn()).isEqualTo("N");
        // 객체 키는 서버가 만든 이름이다 — 올린 이름(공백·한글 포함 가능)을 쓰지 않는다
        String key = saved.getValue().getObjectKey();
        assertThat(key).matches("reference/\\d{6}/[0-9a-f-]{36}\\.pdf");
        assertThat(storage.objects).containsKey(key);
        assertThat(storage.types.get(key)).isEqualTo("application/pdf");
    }

    @Test
    void titleDefaultsToFileNameWithoutExtension() {
        ReferenceDocumentDto dto = service.upload("C:\\Users\\kim\\Desktop\\ER triage guide.PDF", bytes(3), null, null, "emp-1");

        // 브라우저가 전체 경로를 보내도 이름만 쓰고, 확장자는 대소문자를 가리지 않는다
        assertThat(dto.getFileName()).isEqualTo("ER triage guide.PDF");
        assertThat(dto.getTitle()).isEqualTo("ER triage guide");
        assertThat(dto.getCategory()).isNull();
        assertThat(dto.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    void rejectsMissingUploaderAndEmptyFile() {
        assertThatThrownBy(() -> service.upload("a.pdf", bytes(3), null, null, " "))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("uploadedById");
        assertThatThrownBy(() -> service.upload("a.pdf", bytes(0), null, null, "emp-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("file is required");
        assertThatThrownBy(() -> service.upload("a.pdf", null, null, null, "emp-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("file is required");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsFileOverTheSizeLimit() {
        assertThatThrownBy(() -> service.upload("big.pdf", bytes(10 * 1024 * 1024 + 1), null, null, "emp-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at most 10 MB");
        // 한도 정확히는 허용
        assertThat(service.upload("ok.pdf", bytes(10 * 1024 * 1024), null, null, "emp-1").getFileSize())
                .isEqualTo(10L * 1024 * 1024);
    }

    @Test
    void rejectsFileTypesThatCouldRunInTheBrowserOrOnTheServer() {
        for (String name : List.of("page.html", "image.svg", "run.exe", "script.js", "archive.zip", "noextension", "trailingdot.")) {
            assertThatThrownBy(() -> service.upload(name, bytes(3), null, null, "emp-1"))
                    .as(name)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("file type is not allowed");
        }
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void rejectsTooLongTitleCategoryAndFileName() {
        assertThatThrownBy(() -> service.upload("a.pdf", bytes(3), "t".repeat(201), null, "emp-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("title");
        assertThatThrownBy(() -> service.upload("a.pdf", bytes(3), null, "c".repeat(51), "emp-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("category");
        assertThatThrownBy(() -> service.upload("n".repeat(252) + ".pdf", bytes(3), null, null, "emp-1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("file name");
    }

    @Test
    void storageFailureStopsBeforeAnythingIsRegistered() {
        storage.failOnPut = new ExternalServiceException("Document storage is not responding.");

        assertThatThrownBy(() -> service.upload("a.pdf", bytes(3), null, null, "emp-1"))
                .isInstanceOf(ExternalServiceException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void uploadedFileIsRemovedWhenRegistrationFails() {
        when(repository.save(any(ReferenceDocument.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> service.upload("a.pdf", bytes(3), null, null, "emp-1"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(storage.objects).isEmpty();
    }

    // ---------------------------------------------------------------- 목록

    @Test
    void listsDocumentsAndFiltersByKeywordOnTitleCategoryOrFileName() {
        when(repository.findByDeletedYnOrderByUploadedAtDesc("N")).thenReturn(List.of(
                doc("1", "Sepsis bundle", "Protocol", "sepsis_v2.pdf"),
                doc("2", "Triage chart", "Form", "ktas.pdf"),
                doc("3", "Stroke pathway", null, "NIHSS.pdf")));

        assertThat(service.getDocuments(null)).extracting(ReferenceDocumentDto::getId).containsExactly("1", "2", "3");
        assertThat(service.getDocuments("  ")).hasSize(3);
        assertThat(service.getDocuments("SEPSIS")).extracting(ReferenceDocumentDto::getId).containsExactly("1");
        assertThat(service.getDocuments("form")).extracting(ReferenceDocumentDto::getId).containsExactly("2");
        assertThat(service.getDocuments("nihss")).extracting(ReferenceDocumentDto::getId).containsExactly("3");
        assertThat(service.getDocuments("zzz")).isEmpty();
    }

    // ---------------------------------------------------------------- 받기·삭제

    @Test
    void returnsFileContentWithInlineFlagByType() {
        ReferenceDocument pdf = doc("1", "t", null, "guide.pdf");
        ReferenceDocument word = doc("2", "t", null, "form.docx");
        word.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        when(repository.findByIdAndDeletedYn("1", "N")).thenReturn(Optional.of(pdf));
        when(repository.findByIdAndDeletedYn("2", "N")).thenReturn(Optional.of(word));
        storage.objects.put(pdf.getObjectKey(), new byte[]{1, 2, 3});
        storage.objects.put(word.getObjectKey(), new byte[]{4});

        ReferenceDocumentFile first = service.getFile("1");
        assertThat(first.fileName()).isEqualTo("guide.pdf");
        assertThat(first.content()).containsExactly(1, 2, 3);
        assertThat(first.inline()).isTrue();
        // 문서 형식은 브라우저에서 열 수 없으니 내려받게 한다
        assertThat(service.getFile("2").inline()).isFalse();
    }

    @Test
    void deletedOrUnknownDocumentIsNotFound() {
        when(repository.findByIdAndDeletedYn("gone", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFile("gone")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete("gone", "emp-1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteIsAStatusChangeAndKeepsRowAndFile() {
        ReferenceDocument existing = doc("1", "t", null, "guide.pdf");
        when(repository.findByIdAndDeletedYn("1", "N")).thenReturn(Optional.of(existing));
        storage.objects.put(existing.getObjectKey(), new byte[]{1});

        service.delete("1", " emp-2 ");

        assertThat(existing.getDeletedYn()).isEqualTo("Y");
        assertThat(existing.getDeletedById()).isEqualTo("emp-2");
        assertThat(existing.getDeletedAt()).isNotNull();
        verify(repository).save(existing);
        verify(repository, never()).delete(any(ReferenceDocument.class));
        assertThat(storage.objects).containsKey(existing.getObjectKey());
    }

    @Test
    void deleteRequiresWhoDidIt() {
        assertThatThrownBy(() -> service.delete("1", null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("deletedById");
    }
}
