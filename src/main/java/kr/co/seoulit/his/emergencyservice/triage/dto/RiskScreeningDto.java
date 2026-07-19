package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class RiskScreeningDto {
    private Long id;
    private String receptionNo;
    private String screeningTypeCode;
    private BigDecimal score;
    private String resultCode;
    private String screenedById;
    private LocalDateTime screenedAt;
}
