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
    private String verbalYn;
    private List<OrderItemDto> items;
}
