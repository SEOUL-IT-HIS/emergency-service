package kr.co.seoulit.his.emergencyservice.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * 처방 항목. prescriptionType 은 "검사" 또는 "약품"만 쓴다(영상 오더는 처방코어가 받지 않는다).
 * itemId·sendStatus·labOrderId 는 조회 응답에서만 채워지고, 요청에 담아도 처방코어로 전달하지 않는다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemDto {
    private String prescriptionType;
    private String itemCode;
    private String itemName;
    private Double dosage;
    private String dosageFormCd;
    private String frequency;
    private String durationDays;
    private String detailInfo;

    // 응답 전용
    private String itemId;
    private String sendStatus;
    private String labOrderId;
    /** LAB 이 검사 전송을 거절한 사유(예: 유효하지 않은 환자ID, 이미 접수된 오더). 전송이 실패(FAILED)했을 때만 있다 */
    private String rejectReason;

    /**
     * 검사 결과(처방코어가 LAB 결과를 받아 둔 값을 그대로 전달한다 — 응급 DB에 저장하지 않고 열 때마다 처방코어에서 읽는다).
     * 결과가 아직 없으면 resultReportedAt·resultDetails 가 비어 있다.
     */
    private String resultReportedAt;
    private String resultValue;
    private String resultUnit;
    private String referenceRange;
    private String abnormalFlag;
    private java.util.List<ResultDetail> resultDetails;

    /** 결과 한 줄(예: Blood Glucose Test 4 mg/dL, 기준 70-99, 플래그 L) */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResultDetail {
        private Integer seq;
        private String detailCode;
        private String detailName;
        private String resultValue;
        private String resultUnit;
        private String referenceRange;
        /** L(낮음) / H(높음) / N(정상) 등 LAB 이 준 값 */
        private String abnormalFlag;
    }
}
