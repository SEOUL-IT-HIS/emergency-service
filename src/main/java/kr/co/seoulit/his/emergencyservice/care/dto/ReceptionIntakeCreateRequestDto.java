package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReceptionIntakeCreateRequestDto {
    private String receptionId;
    private String patientId;
    private String patientName;
    private String arrivalPath;
    private LocalDateTime receivedAt;
    private String memo;
    private String chiefComplaintRaw;
}
