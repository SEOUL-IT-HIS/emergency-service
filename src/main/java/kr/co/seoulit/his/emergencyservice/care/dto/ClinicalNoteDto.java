package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ClinicalNoteDto {
    private String id;
    private String receptionId;
    private String content;
    private String recordedById;
    private LocalDateTime recordedAt;
}
