package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdmissionRequestCreateDto {
    private String wardPrefer;
    private String targetDeptCode;
    /** 병동에 전달하는 요청 메모(선택, 자유 텍스트, 500자 이내) */
    private String note;
}
