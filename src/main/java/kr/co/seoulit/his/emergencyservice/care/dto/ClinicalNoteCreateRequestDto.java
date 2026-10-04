package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClinicalNoteCreateRequestDto {
    private String encounterId;
    private String noteTypeCode;
    private String content;
    private String recordedById;
}
