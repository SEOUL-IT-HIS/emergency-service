package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class EmergencyPatientDto {
    private String receptionId;
    /** 환자 ID — 같은 환자의 진행 중 접수가 둘 이상인지(중복 접수 의심) 목록에서 알아보는 데 쓴다 */
    private String patientId;
    private String patientName;
    private LocalDateTime receivedAt;
    private String ktasLevelCode;
    private String careStatusCode;
    private String bedNo;
    private String zoneCode;
    private LocalDateTime lastAssessedAt;
    private String memo;
    private String chiefComplaintRaw;
    /** 최신 퇴실 결정의 유형(없으면 null) — 목록의 입원 배지용 */
    private String dispositionTypeCode;
    /** 입원이면 가장 최근 입원요청의 상태(01 요청됨 · 02 병상 배정 · 03 거부). 요청을 아직 보내지 않았으면 null */
    private String admissionStatusCode;
    /** 병동이 배정한 병동(02 일 때) */
    private String assignedWardCode;
}
