package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferNoteCreateDto {
    private String targetHospitalCode;
    private String content;
    private String writtenById;
}
