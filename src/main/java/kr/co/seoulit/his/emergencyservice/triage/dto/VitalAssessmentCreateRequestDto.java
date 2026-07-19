package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class VitalAssessmentCreateRequestDto {
    private String encounterId;
    private String measuredById;
    private List<VitalItemDto> vitals;

    @Getter
    @Setter
    public static class VitalItemDto {
        private Integer systolicBp;
        private Integer heartRate;
        private Integer respRate;
        private BigDecimal temperature;
        private Integer spo2;
        private Integer gcs;
        private BigDecimal ewsScore;
    }
}
