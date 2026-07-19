package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdmissionRequestCreateDto {
    private String wardPrefer;
    private String targetDeptCode;
}
