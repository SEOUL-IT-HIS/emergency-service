package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

/** 약품 목록 한 건 — 처방 등록 때 itemCode·itemName 을 고르는 용도(약제 약품 마스터). */
@Getter
@Setter
public class MedicationDto {
    /** 처방 항목 코드(약품 마스터의 ediCode) */
    private String itemCode;
    private String itemName;
    /** 제형 코드 01(알약·캡슐) / 02(수액) / 03(주사) — 마스터에 비어 있으면 null */
    private String dosageFormCd;
    /** 제형 이름(예: 정제, 주사제) — 없으면 null */
    private String formName;
    /** 제조사 — 없으면 null */
    private String manufacturer;
    /** 일반/전문 의약품 구분 — 없으면 null */
    private String category;
}
