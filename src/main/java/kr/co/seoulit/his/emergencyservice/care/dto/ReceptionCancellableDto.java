package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 접수 취소 가능 여부 — 접수 서비스가 취소 전에 미리 물어 화면에서 안내하는 용도(사전 안내).
 * 조회와 실제 취소 사이에 응급에서 기록이 생길 수 있어서, 최종 판단은 취소 이벤트를 받을 때 응급이 다시 한다.
 */
@Getter
@Setter
public class ReceptionCancellableDto {
    private String receptionId;
    /** 취소해도 되는지(응급에서 취소가 받아들여질지) */
    private boolean cancellable;
    /**
     * CANCELLABLE(기록 없음) · ALREADY_CANCELLED(이미 취소됨) · NOT_FOUND(응급에 없는 접수) — 모두 cancellable=true,
     * HAS_RECORDS(진료 기록 있음) · CANNOT_VERIFY(처방코어에서 처방 여부를 확인하지 못함) — cancellable=false
     */
    private String reasonCode;
    /** reasonCode 가 HAS_RECORDS 일 때 어떤 기록이 있는지(CLINICAL_NOTE, TREATMENT, MEDICATION, CPR, CONSENT, KTAS, VITAL_SIGNS, ISOLATION, RISK_SCREENING, EMS_REFERRAL, BED_ASSIGNMENT(지금 배정 중인 병상), DISPOSITION, ORDER). 아니면 빈 목록 */
    private List<String> records = List.of();
}
