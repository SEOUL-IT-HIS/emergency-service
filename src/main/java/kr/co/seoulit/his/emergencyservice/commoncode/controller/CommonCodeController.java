package kr.co.seoulit.his.emergencyservice.commoncode.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "ER-COMMONCODE 공통코드", description = "admin-service 공통코드를 서버 기동 시 로컬 캐시에 적재해 제공(Provider=ADM, EMG는 캐시 조회만)")
@RestController
@RequestMapping("/api/emergency/codes/common")
@RequiredArgsConstructor
public class CommonCodeController {

    private final CommonCodeCache commonCodeCache;

    @Operation(summary = "캐시된 공통코드 전체 조회", description = "서버 기동 시 admin-service 에서 미리 받아둔 공통코드 전체(그룹코드별)를 한 번에 조회한다")
    @GetMapping
    public ApiResponse<Map<String, List<AdminCommonCodeItemDto>>> getAllCommonCodes() {
        return ApiResponse.success(commonCodeCache.getAll());
    }

    @Operation(summary = "캐시된 공통코드 그룹별 조회", description = "특정 그룹코드 하나만 조회한다. admin 실시간 호출 아님")
    @GetMapping("/{groupCode}")
    public ApiResponse<List<AdminCommonCodeItemDto>> getCommonCodes(@PathVariable String groupCode) {
        return ApiResponse.success(commonCodeCache.get(groupCode));
    }
}
