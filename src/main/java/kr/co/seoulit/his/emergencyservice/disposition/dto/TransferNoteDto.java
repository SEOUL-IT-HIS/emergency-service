package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransferNoteDto {
    private String id;
    private String dispositionId;
    private String targetHospitalCode;
    private String content;
    private String writtenById;
    private LocalDateTime writtenAt;
}
