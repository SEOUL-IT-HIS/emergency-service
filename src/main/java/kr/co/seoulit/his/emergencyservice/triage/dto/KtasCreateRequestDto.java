package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KtasCreateRequestDto {
    private String patientId;
    private String encounterId;
    private String ktasScore;
    private String assessmentTypeCode;
    private String assessedById;
    private String reason;
}
