package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * POST /api/outpatient/prescriptions/emergency/{receptionId} 요청 바디.
 * null 값은 보내지 않는다(verbalYn 처럼 처방코어가 아직 지원하지 않는 필드를 빼기 위해).
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderCoreCreateRequest {
    private String patientId;
    private String prescribedBy;
    private String departmentCode;
    /** 정확히 "ER" — 이 값으로 처방코어가 검사/약제 쪽 채널 구분(encounterType)을 ER 로 세팅한다 */
    private String serviceType;
    private String orderMethod;
    private String priorityCode;
    private String timingCode;
    private String verbalYn;
    private List<Item> items;

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Item {
        private String prescriptionType;
        private String itemCode;
        private String itemName;
        private Double dosage;
        private String dosageFormCd;
        private String frequency;
        private String durationDays;
        private String detailInfo;
    }
}
