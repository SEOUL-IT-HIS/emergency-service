package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmergencyPatientDto {
    private String receptionId;
    /** 환자 ID — 같은 환자의 진행 중 접수가 둘 이상인지(중복 접수 의심) 목록에서 알아보는 데 쓴다 */
    private String patientId;
    private String patientName;
    private LocalDateTime receivedAt;
    private String ktasLevelCode;
    private String careStatusCode;
    private String bedNo;
    private String zoneCode;
    private LocalDateTime lastAssessedAt;
    private String memo;
    private String chiefComplaintRaw;
}
