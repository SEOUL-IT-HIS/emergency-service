package kr.co.seoulit.his.emergencyservice.triage.mapper;

import kr.co.seoulit.his.emergencyservice.triage.dto.*;
import kr.co.seoulit.his.emergencyservice.triage.entity.*;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TriageMapstructMapper {
    EmsReferralDto toEmsDto(EmsReferral entity);
    List<EmsReferralDto> toEmsDtoList(List<EmsReferral> list);
    TriageAssessmentDto toKtasDto(TriageAssessment entity);
    List<TriageAssessmentDto> toKtasDtoList(List<TriageAssessment> list);
    EwsRecordDto toEwsDto(EwsRecord entity);
    List<EwsRecordDto> toEwsDtoList(List<EwsRecord> list);
    IsolationAssessmentDto toIsolationDto(IsolationAssessment entity);
    List<IsolationAssessmentDto> toIsolationDtoList(List<IsolationAssessment> list);
    RiskScreeningDto toRiskDto(RiskScreening entity);
    List<RiskScreeningDto> toRiskDtoList(List<RiskScreening> list);
}