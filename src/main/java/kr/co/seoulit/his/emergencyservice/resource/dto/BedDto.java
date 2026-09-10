package kr.co.seoulit.his.emergencyservice.resource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BedDto {
    private String id;
    private String bedNo;
    private String zoneCode;
    private String bedTypeCode;
    private String bedStatusCode;
}
