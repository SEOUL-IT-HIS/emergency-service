package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TreatmentRecordDto {
    private Long id;
    private String receptionNo;
    private Long orderId;
    private String treatmentTypeCode;
    private String description;
    private String performedById;
    private LocalDateTime performedAt;
}
