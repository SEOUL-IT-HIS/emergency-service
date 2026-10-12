package kr.co.seoulit.his.emergencyservice.referencedoc.controller;

import kr.co.seoulit.his.emergencyservice.common.config.EmergencyExceptionHandler;
import kr.co.seoulit.his.emergencyservice.common.exception.ResourceNotFoundException;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
import kr.co.seoulit.his.emergencyservice.referencedoc.dto.ReferenceDocumentDto;
import kr.co.seoulit.his.emergencyservice.referencedoc.service.ReferenceDocumentFile;
import kr.co.seoulit.his.emergencyservice.referencedoc.service.ReferenceDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReferenceDocumentControllerTest {

    private ReferenceDocumentService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ReferenceDocumentService.class);
        LoginUserResolver loginUser = mock(LoginUserResolver.class);
        when(loginUser.chosenOrLogin(any())).thenAnswer(inv -> inv.getArgument(0) != null ? inv.getArgument(0) : "login-emp");
        mvc = MockMvcBuilders.standaloneSetup(new ReferenceDocumentController(service, loginUser))
                .setControllerAdvice(new EmergencyExceptionHandler())
                .build();
    }

    private static ReferenceDocumentDto dto() {
        ReferenceDocumentDto dto = new ReferenceDocumentDto();
        dto.setId("doc-1");
        dto.setTitle("Sepsis bundle");
        dto.setFileName("sepsis.pdf");
        dto.setContentType("application/pdf");
        dto.setFileSize(5L);
        dto.setUploadedById("emp-1");
        dto.setUploadedAt(LocalDateTime.of(2026, 10, 11, 9, 30));
        return dto;
    }

    @Test
    void listPassesKeywordAndWrapsInApiResponse() throws Exception {
        when(service.getDocuments("sepsis")).thenReturn(List.of(dto()));

        mvc.perform(get("/api/emergency/reference-documents").param("keyword", "sepsis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].id").value("doc-1"))
                .andExpect(jsonPath("$.data[0].title").value("Sepsis bundle"));
    }

    @Test
    void uploadSendsFileBytesAndFallsBackToLoginUser() throws Exception {
        when(service.upload(eq("sepsis.pdf"), any(), eq("Sepsis bundle"), eq("Protocol"), eq("login-emp"))).thenReturn(dto());
        MockMultipartFile file = new MockMultipartFile("file", "sepsis.pdf", "application/pdf", new byte[]{1, 2, 3});

        mvc.perform(multipart("/api/emergency/reference-documents").file(file)
                        .param("title", "Sepsis bundle").param("category", "Protocol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("doc-1"));

        verify(service).upload(eq("sepsis.pdf"), eq(new byte[]{1, 2, 3}), eq("Sepsis bundle"), eq("Protocol"), eq("login-emp"));
    }

    @Test
    void uploadWithoutFileIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/emergency/reference-documents").param("title", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMG_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("file is required"));
    }

    @Test
    void fileIsServedInlineForPdfWithNosniff() throws Exception {
        when(service.getFile("doc-1")).thenReturn(new ReferenceDocumentFile("패혈증 지침.pdf", "application/pdf", new byte[]{1, 2}, true));

        mvc.perform(get("/api/emergency/reference-documents/doc-1/file"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.startsWith("inline;")))
                // 한글 파일 이름은 RFC 5987 로 인코딩된다
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("filename*=UTF-8''")))
                .andExpect(content().bytes(new byte[]{1, 2}));
    }

    @Test
    void nonViewableTypeIsServedAsAttachment() throws Exception {
        when(service.getFile("doc-2")).thenReturn(new ReferenceDocumentFile("form.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{3}, false));

        mvc.perform(get("/api/emergency/reference-documents/doc-2/file"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.startsWith("attachment;")));
    }

    @Test
    void unknownDocumentIs404() throws Exception {
        when(service.getFile("none")).thenThrow(new ResourceNotFoundException("reference document not found: none"));

        mvc.perform(get("/api/emergency/reference-documents/none/file"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMG_NOT_FOUND"));
    }

    @Test
    void deleteUsesGivenOrLoginUser() throws Exception {
        mvc.perform(delete("/api/emergency/reference-documents/doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        mvc.perform(delete("/api/emergency/reference-documents/doc-1").param("deletedById", "emp-9"))
                .andExpect(status().isOk());

        verify(service).delete("doc-1", "login-emp");
        verify(service).delete("doc-1", "emp-9");
    }
}
