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
}
