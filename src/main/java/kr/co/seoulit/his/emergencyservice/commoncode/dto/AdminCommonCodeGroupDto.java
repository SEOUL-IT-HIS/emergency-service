package kr.co.seoulit.his.emergencyservice.commoncode.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * admin-service 공통코드 "그룹" 응답 항목.
 * GET /api/commonCodeGroup/list 응답 — hisfrontend 의
 * features/commonCode/types/commonCodeGroupTypes.ts (CommonCodeGroup) 과 필드 맞춤.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdminCommonCodeGroupDto {
    private Long groupId;
    private String groupCode;
    private String groupName;
    private String useYn;
}
