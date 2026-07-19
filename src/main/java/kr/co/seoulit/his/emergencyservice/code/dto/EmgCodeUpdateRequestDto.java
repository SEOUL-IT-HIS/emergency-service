package kr.co.seoulit.his.emergencyservice.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeUpdateRequestDto {
    private String codeName;
    private String description;
    private Integer sortOrder;
}
