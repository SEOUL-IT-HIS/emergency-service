package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CprTimelineCreateRequestDto {
    private String encounterId;
    private String outcomeCode;
    private List<CprEventItemDto> events;

    @Getter
    @Setter
    public static class CprEventItemDto {
        private LocalDateTime eventAt;
        private String eventTypeCode;
        private String detail;
        private String recordedById;
    }
}
