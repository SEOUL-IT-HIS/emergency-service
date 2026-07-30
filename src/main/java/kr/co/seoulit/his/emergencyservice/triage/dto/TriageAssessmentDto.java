package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TriageAssessmentDto {
    private String id;
    private String receptionNo;
    private String ktasLevelCode;
    private String assessmentTypeCode;
    private String assessedById;
    private LocalDateTime assessedAt;
    private String reason;
}
