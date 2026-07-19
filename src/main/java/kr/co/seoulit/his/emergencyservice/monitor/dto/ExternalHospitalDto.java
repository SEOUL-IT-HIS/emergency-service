package kr.co.seoulit.his.emergencyservice.monitor.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalHospitalDto {
    private String hospitalCode;
    private String hospitalName;
    private String region;
    private Integer availableBeds;
}
