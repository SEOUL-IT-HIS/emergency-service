package kr.co.seoulit.his.emergencyservice.care.controller;

import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordDto;
import kr.co.seoulit.his.emergencyservice.care.service.ConsentService;
import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-CARE 응급진료", description = "응급환자 목록, 진료기록·처치·투약(MAR)·CPR·동의 기록 (Provider=EMG). 처치·MAR의 orderId는 GR2 참조")
@RestController
@RequestMapping("/api/emergency/care/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    // 기록자는 화면에서 고른 직원이고, 비어 있으면 로그인한 사용자로 채운다
    private final LoginUserResolver loginUser;

    @Operation(summary = "동의 기록 등록", description = "동의 기록 · 종이 동의서를 받은 사실만 기록(서명·파일 저장 없음). 유예는 consentStatusCode=DEFERRED + reason")
    @PostMapping
    public ApiResponse<ConsentRecordDto> createConsent(@RequestBody ConsentRecordCreateRequestDto request) {
        request.setRecordedById(loginUser.chosenOrLogin(request.getRecordedById()));
        return ApiResponse.success(consentService.createConsent(request));
    }

    @Operation(summary = "동의 기록 목록 조회", description = "접수 건별 동의 기록(수령 일시 최신순)")
    @GetMapping
    public ApiResponse<List<ConsentRecordDto>> getConsents(@RequestParam String receptionId) {
        return ApiResponse.success(consentService.getConsents(receptionId));
    }
}
