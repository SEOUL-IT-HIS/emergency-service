package kr.co.seoulit.his.emergencyservice.code.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmgCodeDto {
    private Long id;
    private String groupCode;
    private String codeValue;
    private String codeName;
    private String description;
    private Integer sortOrder;
    private String useYn;
}
