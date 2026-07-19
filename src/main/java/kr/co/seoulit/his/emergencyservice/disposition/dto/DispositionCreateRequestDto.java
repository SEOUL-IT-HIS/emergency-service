package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositionCreateRequestDto {
    private String encounterId;
    private String dispositionType;
    private String decidedById;
}
