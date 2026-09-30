package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import java.util.List;

public interface CareService {
    List<EmergencyPatientDto> getPatients(String date, String status);
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
