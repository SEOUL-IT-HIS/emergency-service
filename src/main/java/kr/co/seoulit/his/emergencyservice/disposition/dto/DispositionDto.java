package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class DispositionDto {
    private String id;
    private String receptionId;
    private String dispositionTypeCode;
    private String decidedById;
    private LocalDateTime decidedAt;
}
