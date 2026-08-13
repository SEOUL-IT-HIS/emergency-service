package kr.co.seoulit.his.emergencyservice.commoncode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * admin-service 공통코드 응답 항목.
 * 필드명은 emergency-service 자체 EmgCodeDto 네이밍(groupCode/codeValue/codeName/sortOrder/useYn)
 * 컨벤션을 따라 추정한 것 — admin 팀과 실제 스펙 확정 필요.
 * 알 수 없는 필드가 와도 역직렬화가 깨지지 않도록 무시한다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminCommonCodeDto {
    private String groupCode;
    private String codeValue;
    private String codeName;
    private Integer sortOrder;
    private String useYn;
}
