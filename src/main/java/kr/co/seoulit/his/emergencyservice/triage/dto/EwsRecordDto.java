package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class EwsRecordDto {
    private String id;
    private String receptionNo;
    private Integer systolicBp;
    private Integer heartRate;
    private Integer respRate;
    private BigDecimal temperature;
    private Integer spo2;
    private Integer gcs;
    private BigDecimal ewsScore;
    private String measuredById;
    private LocalDateTime measuredAt;
}
