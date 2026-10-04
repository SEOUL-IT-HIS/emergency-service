package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TreatmentRecordDto {
    private String id;
    private String receptionId;
    private String orderId;
    private String treatmentTypeCode;
    private String description;
    private String performedById;
    private LocalDateTime performedAt;
}
