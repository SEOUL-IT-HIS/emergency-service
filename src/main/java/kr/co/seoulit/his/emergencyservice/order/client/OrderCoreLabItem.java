package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 처방코어 검사항목 검색 응답 한 건(LAB팀 확정 계약). 모르는 필드는 무시한다. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCoreLabItem {
    private String itemCode;
    private String itemName;
    /** GENERAL / MICROBIOLOGY / PATHOLOGY */
    private String testClassification;
    /** 허용 검체 종류 목록 */
    private List<String> specimenTypes;
}
