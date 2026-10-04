package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import java.util.List;

public interface CareService {
    List<EmergencyPatientDto> getPatients(String date, String status);
    /** 경고 대상으로 보는 기본 기간(시간). 이보다 오래된 미퇴실 접수는 퇴실 누락·테스트 데이터로 보고 제외한다. */
    int DEFAULT_ACTIVE_WINDOW_HOURS = 48;

    /** sinceHours 가 null 이면 기본값(48시간). */
    List<ActiveReceptionDto> getActiveReceptions(String patientId, Integer sinceHours);
    List<ClinicalNoteDto> getRecords(String receptionId);
    ClinicalNoteDto createRecord(ClinicalNoteCreateRequestDto request);
    List<TreatmentRecordDto> getTreatments(String receptionId);
    TreatmentRecordDto createTreatment(TreatmentCreateRequestDto request);
    List<MarDto> getMars(String receptionId);
    MarDto createMar(MarCreateRequestDto request);
    List<CprEventDto> getCprEvents(String receptionId);
    CprEventDto createCprTimeline(CprTimelineCreateRequestDto request);
    ReceptionIntakeDto createReceptionIntake(ReceptionIntakeCreateRequestDto request);
}
