package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class MarDto {
    private String id;
    private String receptionId;
    private Long orderId;
    private Long orderItemId;
    private String drugCode;
    private String dose;
    private String routeCode;
    private String administeredById;
    private LocalDateTime administeredAt;
}
