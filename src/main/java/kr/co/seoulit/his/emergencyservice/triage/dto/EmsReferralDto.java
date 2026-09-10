package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmsReferralDto {
    private String id;
    private String receptionId;
    private String emsAgencyName;
    private String vitalsOnScene;
    private String prehospitalTreatment;
    private LocalDateTime transmittedAt;
}
