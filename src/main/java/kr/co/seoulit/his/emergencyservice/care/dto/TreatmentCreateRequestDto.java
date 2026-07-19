package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TreatmentCreateRequestDto {
    private String encounterId;
    private Long orderId;
    private String treatmentCode;
    private String description;
    private String performedById;
}
