package kr.co.seoulit.his.emergencyservice.triage.service;

import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import java.util.List;

public interface TriageService {
    List<EmsReferralDto> getEmsInfo(String receptionId);
    List<TriageAssessmentDto> getKtasHistory(String receptionId);
    TriageAssessmentDto createKtas(KtasCreateRequestDto request);
    TriageAssessmentDto updateKtas(String id, KtasUpdateRequestDto request);
    List<EwsRecordDto> getVitalAssessments(String receptionId);
    List<EwsRecordDto> createVitalAssessments(VitalAssessmentCreateRequestDto request);
    List<IsolationAssessmentDto> getIsolations(String receptionId);
    IsolationAssessmentDto createIsolation(IsolationCreateRequestDto request);
    IsolationAssessmentDto releaseIsolation(String id);
    List<RiskScreeningDto> getRiskScreenings(String receptionId);
    RiskScreeningDto createRiskScreening(RiskScreeningCreateRequestDto request);
}
