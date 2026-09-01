package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmergencyPatientDto {
    private String receptionNo;
    private String patientName;
    private String ktasLevelCode;
    private String careStatusCode;
    private String bedNo;
    private String zoneCode;
    private LocalDateTime lastAssessedAt;
}
