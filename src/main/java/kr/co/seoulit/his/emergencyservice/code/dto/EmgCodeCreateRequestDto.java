package kr.co.seoulit.his.emergencyservice.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeCreateRequestDto {
    private String groupCode;
    private String groupName;
    private String groupDescription;
    private String codeValue;
    private String codeName;
    private String description;
    private Integer sortOrder;
}
