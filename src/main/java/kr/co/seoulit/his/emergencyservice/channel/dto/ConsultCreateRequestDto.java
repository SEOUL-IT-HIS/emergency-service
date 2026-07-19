package kr.co.seoulit.his.emergencyservice.channel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConsultCreateRequestDto {
    private String encounterId;
    private String specialty;
    private String reason;
    private Long orderId;
    private String channelRef;
}
