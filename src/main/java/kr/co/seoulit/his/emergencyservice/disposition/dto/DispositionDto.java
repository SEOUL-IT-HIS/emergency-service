package kr.co.seoulit.his.emergencyservice.disposition.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class DispositionDto {
    private String id;
    private String receptionId;
    private String dispositionTypeCode;
    private String decidedById;
    private LocalDateTime decidedAt;
    /** 이 결정을 다른 유형으로 바꿀 수 있는지(최신 결정이고 후속 조치 전일 때만 true) */
    private boolean changeable;
    /**
     * 이 접수의 퇴실 진행 단계(최신 결정에만): NONE / OPEN / WAITING_WARD / DONE. 화면이 퇴실 완료(DONE) 환자에게
     * 병상 배정·평가·처방 입력을 미리 막는 데 쓴다(백엔드도 같은 기준으로 409 로 막는다).
     */
    private String stage;
}
