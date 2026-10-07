package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class    ReceptionIntakeCreateRequestDto {
    private String receptionId;
    private String patientId;
    private String patientName;
    private String arrivalPath;
    private LocalDateTime receivedAt;
    private String memo;
    private String chiefComplaintRaw;
    /** 접수에서 입력한 KTAS 등급(1~5 또는 01~05). 있으면 최초(INITIAL) 분류로 저장한다 */
    @com.fasterxml.jackson.annotation.JsonAlias({"ktasLevelCode", "ktasScore", "ktas"})
    private String ktasLevel;
    /** 접수에서 분류한 시각. 없으면 접수 시각 */
    private LocalDateTime triageDateTime;
}
