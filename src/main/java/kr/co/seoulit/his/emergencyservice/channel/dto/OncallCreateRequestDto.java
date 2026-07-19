package kr.co.seoulit.his.emergencyservice.channel.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OncallCreateRequestDto {
    private String encounterId;
    private String targetRole;
    private String calledById;
}
