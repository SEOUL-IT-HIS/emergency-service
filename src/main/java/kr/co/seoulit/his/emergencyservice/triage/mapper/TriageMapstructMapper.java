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
    EwsRecordDto toEwsDto(EwsRecord entity);
    List<EwsRecordDto> toEwsDtoList(List<EwsRecord> list);
    IsolationAssessmentDto toIsolationDto(IsolationAssessment entity);
    RiskScreeningDto toRiskDto(RiskScreening entity);
}