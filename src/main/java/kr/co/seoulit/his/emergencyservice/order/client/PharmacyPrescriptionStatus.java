package kr.co.seoulit.his.emergencyservice.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * 약제(PHM) 조제 상태 조회 결과 한 건 — GET /api/pharmacy/prescriptions?prescriptionId=
 * 약제가 알려 주는 값만 담고 응급 DB에는 저장하지 않는다(조회할 때마다 약제에서 읽는다).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PharmacyPrescriptionStatus {
    /** 처방ID — 응답에 있으면 우리 처방과 같은지 확인하는 데 쓴다 */
    private String prescriptionId;
    /** RECEIVED(접수) / DISPENSED(조제완료) / REJECTED(거절) / CANCELLED(처방 취소) */
    private String status;
    /** 조제완료 건의 불출 상태: RELEASED / CANCELLED / null */
    private String releaseStatusCd;
    /** 처방코어 취소 통보의 처리 결과: APPLIED(반영됨) / REFUSED(불출 이후라 미반영) / null(통보 없음) */
    private String cancelOutcome;
}
