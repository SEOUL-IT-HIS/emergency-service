package kr.co.seoulit.his.emergencyservice.commoncode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * admin-service 공통코드 "항목" 응답 항목.
 * GET /api/commonCodeItem/list?groupId= 응답 — hisfrontend 의
 * features/commonCode/types/commonCodeItemTypes.ts (CommonCodeItem) 과 필드 맞춤.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminCommonCodeItemDto {
    private Long codeId;
    private Long groupId;
    private String codeValue;
    private String codeName;
    private String useYn;
}
