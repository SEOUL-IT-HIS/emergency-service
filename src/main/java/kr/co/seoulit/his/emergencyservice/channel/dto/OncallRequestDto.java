package kr.co.seoulit.his.emergencyservice.channel.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class OncallRequestDto {
    private Long id;
    private String receptionNo;
    private String targetRoleCode;
    private String calledById;
    private LocalDateTime calledAt;
}
