package kr.co.seoulit.his.emergencyservice.patient.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatientBatchRequestDto {
    private List<String> patientIds;

    public PatientBatchRequestDto(List<String> patientIds){
        this.patientIds = patientIds;
    }
}
