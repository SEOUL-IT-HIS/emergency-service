package kr.co.seoulit.his.emergencyservice.triage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KtasUpdateRequestDto {
    private String ktasScore;
    private String assessedById;
    private String reason;
}
