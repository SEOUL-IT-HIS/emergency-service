package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmsReferralDto {
    private Long id;
    private String receptionNo;
    private String emsAgencyName;
    private String vitalsOnScene;
    private String prehospitalTreatment;
    private LocalDateTime transmittedAt;
}
