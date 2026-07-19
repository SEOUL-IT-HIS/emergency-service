package kr.co.seoulit.his.emergencyservice.code.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.code.dto.*;
import kr.co.seoulit.his.emergencyservice.code.service.EmgCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-CODE 응급업무코드", description = "응급 전용 코드그룹·코드값 CRUD (Provider=EMG). 공통코드는 admin-service 사용, EMG는 전용코드만 소유")
@RestController
@RequestMapping("/api/emergency/codes")
@RequiredArgsConstructor
public class EmgCodeController {

    private final EmgCodeService emgCodeService;

    @Operation(summary = "응급업무코드 조회", description = "UC-CODE-01 · 응급 전용 코드그룹·코드값 목록(공통코드 제외)")
    @GetMapping
    public ApiResponse<List<EmgCodeGroupDto>> getCodes() {
        return ApiResponse.success(emgCodeService.getCodes());
    }

    @Operation(summary = "응급업무코드 그룹별 조회", description = "UC-CODE-02 · 지정 그룹의 코드값 목록")
    @GetMapping("/{groupCode}")
    public ApiResponse<List<EmgCodeDto>> getCodesByGroup(@PathVariable String groupCode) {
        return ApiResponse.success(emgCodeService.getCodesByGroup(groupCode));
    }

    @Operation(summary = "응급업무코드 등록", description = "UC-CODE-03 · 전용 코드그룹/코드값 신규 등록")
    @PostMapping
    public ApiResponse<EmgCodeDto> createCode(@RequestBody EmgCodeCreateRequestDto request) {
        return ApiResponse.success(emgCodeService.createCode(request));
    }

    @Operation(summary = "응급업무코드 수정", description = "UC-CODE-04 · 코드명·설명·정렬순서 수정(코드값 변경 불가)")
    @PutMapping("/{codeId}")
    public ApiResponse<EmgCodeDto> updateCode(
            @PathVariable Long codeId,
            @RequestBody EmgCodeUpdateRequestDto request) {
        return ApiResponse.success(emgCodeService.updateCode(codeId, request));
    }

    @Operation(summary = "응급업무코드 사용여부 변경", description = "UC-CODE-05 · 사용중지/사용 (삭제 대체)")
    @PutMapping("/{codeId}/use-yn")
    public ApiResponse<EmgCodeDto> updateUseYn(
            @PathVariable Long codeId,
            @RequestBody EmgCodeUseYnRequestDto request) {
        return ApiResponse.success(emgCodeService.updateUseYn(codeId, request));
    }
}
