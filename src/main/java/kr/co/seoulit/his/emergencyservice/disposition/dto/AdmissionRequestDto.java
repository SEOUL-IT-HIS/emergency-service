package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class AdmissionRequestDto {
    private String id;
    private String dispositionId;
    private String targetDeptCode;
    private String requestStatusCode;
    private LocalDateTime requestedAt;
}
