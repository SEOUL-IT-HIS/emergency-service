package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CprEventDto {
    private Long id;
    private String receptionNo;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String outcomeCode;
    private List<CprTimelineItemDto> timelines;

    @Getter
    @Setter
    public static class CprTimelineItemDto {
        private Long id;
        private LocalDateTime eventAt;
        private String eventTypeCode;
        private String detail;
        private String recordedById;
    }
}
