package kr.co.seoulit.his.emergencyservice.triage.service;

import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import java.util.List;

public interface TriageService {
    List<EmsReferralDto> getEmsInfo(String receptionNo);
    List<TriageAssessmentDto> getKtasHistory(String receptionNo);
    TriageAssessmentDto createKtas(KtasCreateRequestDto request);
    TriageAssessmentDto updateKtas(String id, KtasUpdateRequestDto request);
    List<EwsRecordDto> getVitalAssessments(String receptionNo);
    List<EwsRecordDto> createVitalAssessments(VitalAssessmentCreateRequestDto request);
    List<IsolationAssessmentDto> getIsolations(String receptionNo);
    IsolationAssessmentDto createIsolation(IsolationCreateRequestDto request);
    IsolationAssessmentDto releaseIsolation(String id);
    List<RiskScreeningDto> getRiskScreenings(String receptionNo);
    RiskScreeningDto createRiskScreening(RiskScreeningCreateRequestDto request);
}
