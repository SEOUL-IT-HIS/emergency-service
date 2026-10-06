package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/** 처방코어 약품 검색 응답 한 건(약품 마스터). 응급이 쓰는 필드만 받고 모르는 필드는 무시한다. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCoreMedication {
    private String medicationId;
    private String medicationName;
    /** 처방 항목 코드로 쓰는 약품 코드(외래 처방 화면도 이 값을 itemCode 로 쓴다) */
    private String ediCode;
    /** 제형 이름(예: 정제, 주사제) */
    private String formCodeName;
    /** 제조사 */
    private String entpName;
    /** 일반/전문 의약품 구분 */
    private String etcOtcName;
}
