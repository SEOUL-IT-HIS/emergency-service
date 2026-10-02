package kr.co.seoulit.his.emergencyservice.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 검사항목 검색 결과 — 처방 등록 때 itemCode·itemName 을 고르는 용도(처방코어가 LAB팀 계약대로 내려준 값). */
@Getter
@Setter
public class LabItemDto {
    private String itemCode;
    private String itemName;
    /** GENERAL / MICROBIOLOGY / PATHOLOGY */
    private String testClassification;
    private List<String> specimenTypes;
}
