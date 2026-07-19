package kr.co.seoulit.his.emergencyservice.triage.service;

import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import java.util.List;

public interface TriageService {
    List<EmsReferralDto> getEmsInfo(String receptionNo);
    TriageAssessmentDto createKtas(KtasCreateRequestDto request);
    TriageAssessmentDto updateKtas(Long id, KtasUpdateRequestDto request);
    List<EwsRecordDto> createVitalAssessments(VitalAssessmentCreateRequestDto request);
    IsolationAssessmentDto createIsolation(IsolationCreateRequestDto request);
    RiskScreeningDto createRiskScreening(RiskScreeningCreateRequestDto request);
}
