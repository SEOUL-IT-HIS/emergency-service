package kr.co.seoulit.his.emergencyservice.care.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 한 환자의 진행 중(퇴실 처리 전)인 응급 접수 — 접수 서비스가 중복 접수 경고에 쓴다. */
@Getter
@Setter
public class ActiveReceptionDto {
    private String receptionId;
    private String patientId;
    private LocalDateTime receivedAt;
}
