package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ConsentRecordCreateRequestDto {
    private String encounterId;
    private String consentTypeCode;
    private String consentStatusCode;
    private String consentedByCode;
    private String consenterName;
    private String reason;
    private LocalDateTime receivedAt;
    private String recordedById;
}
