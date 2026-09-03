package kr.co.seoulit.his.emergencyservice.channel.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ConsultRequestDto {
    private String id;
    private String receptionId;
    private String targetDeptCode;
    private String consultStatusCode;
    private String reason;
    private Long orderId;
    private LocalDateTime requestedAt;
}
