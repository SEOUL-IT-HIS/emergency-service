package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class IsolationAssessmentDto {
    private String id;
    private String receptionId;
    private String isolationTypeCode;
    private String requiredYn;
    private String decidedById;
    private LocalDateTime decidedAt;
    private LocalDateTime releasedAt;
}
