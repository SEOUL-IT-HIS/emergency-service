package kr.co.seoulit.his.emergencyservice.code.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class EmgCodeGroupDto {
    private Long id;
    private String groupCode;
    private String groupName;
    private String description;
    private String useYn;
    private Integer sortOrder;
    private List<EmgCodeDto> codes;
}
