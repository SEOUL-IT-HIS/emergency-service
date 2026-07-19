package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IsolationCreateRequestDto {
    private String patientId;
    private String encounterId;
    private String isolationTypeCode;
    private String requiredYn;
    private String decidedById;
}
