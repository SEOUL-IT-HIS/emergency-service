package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kr.co.seoulit.his.emergencyservice.order.dto.OrderItemDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 처방코어 PrescriptionDto 중 응급이 쓰는 필드(2026-10-01 /api-docs 기준 + receptionId).
 * 모르는 필드는 무시한다. 시각은 형식이 달라도 깨지지 않게 문자열로 받는다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderCorePrescription {
    private String prescriptionId;
    private String receptionId;
    private String patientId;
    private String serviceType;
    private String status;
    private String prescribedAt;
    private String prescribedBy;
    private String cancelledAt;
    private String cancelReason;
    private String orderMethod;
    private String priorityCode;
    private String timingCode;
    /** 처방방법 이름(예: 01 → Electronic) — 처방코어가 코드와 함께 내려준다 */
    private String orderMethodName;
    private String verbalYn;
    private String verbalConfirmedAt;
    private String verbalConfirmedBy;
    private List<OrderItemDto> items;
    /** 목록 조회(receptionId)에서 내려오는 전송 상태 요약. 약제는 처방 단위(PENDING/SENT/FAILED) */
    private String pharmacySendStatus;
    /** 검사 항목 전송 상태 요약: 하나라도 FAILED면 FAILED, 미전송/PENDING이 있으면 PENDING, 전부 SENT면 SENT, 검사 항목이 없으면 null */
    private String labSendStatus;
}
