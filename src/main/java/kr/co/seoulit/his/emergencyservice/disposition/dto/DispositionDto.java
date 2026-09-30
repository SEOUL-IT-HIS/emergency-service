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
    /** 이 결정을 다른 유형으로 바꿀 수 있는지(최신 결정이고 후속 조치 전일 때만 true) */
    private boolean changeable;
}
