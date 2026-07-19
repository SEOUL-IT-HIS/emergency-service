package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import java.util.List;

public interface CareService {
    List<EmergencyPatientDto> getPatients(String date, String status);
    ClinicalNoteDto createRecord(ClinicalNoteCreateRequestDto request);
    TreatmentRecordDto createTreatment(TreatmentCreateRequestDto request);
    MarDto createMar(MarCreateRequestDto request);
    CprEventDto createCprTimeline(CprTimelineCreateRequestDto request);
}
