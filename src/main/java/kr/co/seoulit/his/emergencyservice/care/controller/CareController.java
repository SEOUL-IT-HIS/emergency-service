package kr.co.seoulit.his.emergencyservice.care.controller;

import kr.co.seoulit.his.emergencyservice.common.ApiResponse;
import kr.co.seoulit.his.emergencyservice.common.session.LoginUserResolver;
import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.service.CareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ER-CARE 응급진료", description = "응급환자 목록, 진료기록·처치·투약(MAR)·CPR 기록 (Provider=EMG). 처치·MAR의 orderId는 GR2 참조")
@RestController
@RequestMapping("/api/emergency/care")
@RequiredArgsConstructor
public class CareController {

    private final CareService careService;
    // 기록자(진료기록·CPR 이벤트)는 로그인한 사용자로 기록한다 — 요청에 실린 값은 바꿔 보낼 수 있다.
    // 처치 시행자(performedById)·투약 투여자(administeredById)는 실제로 한 사람이 기록하는 사람과 다를 수 있어
    // 화면에서 고른 직원(기본은 로그인한 사람)을 요청값 그대로 쓴다.
    private final LoginUserResolver loginUser;

    @Operation(summary = "응급환자 목록 조회", description = "UC-CARE-01 · 접수 유입 환자목록 + 초기 임상정보 (연계:RCP)")
    @GetMapping("/patients")
    public ApiResponse<List<EmergencyPatientDto>> getPatients(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(careService.getPatients(date, status));
    }

    @Operation(summary = "환자의 진행 중인 응급 접수 조회",
            description = "접수 서비스가 같은 환자 중복 접수를 경고하기 위해 호출한다. 접수 후 sinceHours(기본 48)시간 이내이면서 퇴실 처리 전인 접수 목록(접수 시각 오름차순), 없으면 빈 목록")
    @GetMapping("/patients/active")
    public ApiResponse<List<ActiveReceptionDto>> getActiveReceptions(
            @RequestParam String patientId,
            @RequestParam(required = false) Integer sinceHours) {
        return ApiResponse.success(careService.getActiveReceptions(patientId, sinceHours));
    }

    @Operation(summary = "응급 진료기록 목록 조회", description = "UC-CARE-02 · 특정 접수건의 EMR 임상노트 전체 조회")
    @GetMapping("/records")
    public ApiResponse<List<ClinicalNoteDto>> getRecords(@RequestParam String receptionId){
        return ApiResponse.success(careService.getRecords(receptionId));
    }

    @Operation(summary = "응급 진료기록 입력", description = "UC-CARE-02 · EMR 임상노트")
    @PostMapping("/records")
    public ApiResponse<ClinicalNoteDto> createRecord(@RequestBody ClinicalNoteCreateRequestDto request) {
        request.setRecordedById(loginUser.actorOr(request.getRecordedById()));
        return ApiResponse.success(careService.createRecord(request));
    }

    @Operation(summary = "응급 처치 기록 목록 조회", description = "UC-CARE-03 · 접수 건별 처치기록(시행 시각 순)")
    @GetMapping("/treatments")
    public ApiResponse<List<TreatmentRecordDto>> getTreatments(@RequestParam String receptionId) {
        return ApiResponse.success(careService.getTreatments(receptionId));
    }

    @Operation(summary = "응급 처치 기록", description = "UC-CARE-03 · 처치기록(orderId=GR2 참조 권장)")
    @PostMapping("/treatments")
    public ApiResponse<TreatmentRecordDto> createTreatment(@RequestBody TreatmentCreateRequestDto request) {
        return ApiResponse.success(careService.createTreatment(request));
    }

    @Operation(summary = "약물 투여 기록(MAR) 목록 조회", description = "UC-CARE-04 · 접수 건별 투여기록(투여 시각 순)")
    @GetMapping("/medication-administrations")
    public ApiResponse<List<MarDto>> getMars(@RequestParam String receptionId) {
        return ApiResponse.success(careService.getMars(receptionId));
    }

    @Operation(summary = "약물 투여 기록(MAR)", description = "UC-CARE-04 · 투여기록. 처방자장은 GR2, orderId 필수 권장")
    @PostMapping("/medication-administrations")
    public ApiResponse<MarDto> createMar(@RequestBody MarCreateRequestDto request) {
        return ApiResponse.success(careService.createMar(request));
    }

    @Operation(summary = "CPR 타임라인 목록 조회", description = "UC-CARE-05 · 접수 건별 CPR 기록(최신 시작 순, 타임라인은 이벤트 시각 순)")
    @GetMapping("/cpr-timelines")
    public ApiResponse<List<CprEventDto>> getCprEvents(@RequestParam String receptionId) {
        return ApiResponse.success(careService.getCprEvents(receptionId));
    }

    @Operation(summary = "CPR 타임라인 기록", description = "UC-CARE-05 · 심폐소생술 이벤트 타임라인")
    @PostMapping("/cpr-timelines")
    public ApiResponse<CprEventDto> createCprTimeline(@RequestBody CprTimelineCreateRequestDto request) {
        if (request.getEvents() != null) {
            for (CprTimelineCreateRequestDto.CprEventItemDto event : request.getEvents()) {
                if (event != null) {
                    event.setRecordedById(loginUser.actorOr(event.getRecordedById()));
                }
            }
        }
        return ApiResponse.success(careService.createCprTimeline(request));
    }

    @Operation(summary = "응급접수 정보 수신", description = "UC-CARE-01 보조 · RCP가 응급 접수 발생 시 호출(연계:RCP). 재전송 시 upsert")
    @PostMapping("/reception-intakes")
    public ApiResponse<ReceptionIntakeDto> createReceptionIntake(@RequestBody ReceptionIntakeCreateRequestDto request) {
        return ApiResponse.success(careService.createReceptionIntake(request));
    }
}
