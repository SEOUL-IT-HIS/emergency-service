package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/** 약제(PHM) 약품 마스터 한 건 — GET /api/pharmacy/medications/page 응답의 content 한 줄. 응급 DB에는 저장하지 않는다. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PharmacyMedication {
    /** 약가(EDI) 코드 — 처방 항목 코드로 쓴다. 코드가 없는 약품은 처방 접수가 안 된다 */
    private String ediCode;
    private String medicationName;
    /** 제형 코드 01(알약·캡슐) / 02(수액) / 03(주사) — 비어 있을 수 있다 */
    private String dosageFormCd;
    /** 제형 이름(예: 정제, 주사제) */
    private String formCodeName;
    private String entpName;
    private String etcOtcName;
}
