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
    /** 병동이 배정한 병동(WARD_CD 값). 병상 배정 완료 회신 뒤에만 있다 */
    private String assignedWardCode;
    private LocalDateTime requestedAt;
}
