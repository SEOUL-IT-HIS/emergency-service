package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class RiskScreeningCreateRequestDto {
    private String encounterId;
    private String screenType;
    private BigDecimal score;
    private String resultCode;
    private String screenedById;
}
