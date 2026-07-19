package kr.co.seoulit.his.emergencyservice.monitor.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class LosAlertDto {
    private Long id;
    private String receptionNo;
    private Integer thresholdMinutes;
    private LocalDateTime triggeredAt;
}
