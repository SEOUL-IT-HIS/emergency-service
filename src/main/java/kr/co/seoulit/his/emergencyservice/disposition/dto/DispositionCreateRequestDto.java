package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DispositionCreateRequestDto {
    private String encounterId;
    private String dispositionType;
    private String decidedById;
    /** 오접수·중복 접수(06)일 때만: 사유 유형 DUPLICATE / ERRONEOUS (필수), 사유(필수, 500자 이내), 중복일 때 남기는 접수 ID(선택) */
    private String voidReasonType;
    private String voidReason;
    private String keptReceptionId;
}
