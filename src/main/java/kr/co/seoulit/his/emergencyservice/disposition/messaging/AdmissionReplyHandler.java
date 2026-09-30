package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.disposition.service.DispositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 병동 회신(BED_ASSIGNED / ADMISSION_REJECTED)을 입원요청 상태로 반영한다.
 * 회신 JSON 은 {dispositionId, ...}. 이 클래스는 Kafka 없이도 단위 테스트할 수 있게 리스너와 분리했다.
 * 병동팀 합의(2026-09-30): 처음 배정 회신만 반영한다(병상 변경·배정 취소·입원요청 취소 이벤트는 없다).
 * 화면의 병동은 희망 병동이 아니라 BED_ASSIGNED 회신의 wardCode 로 보여준다(다른 병동으로 배정될 수 있음).
 * 병상 번호(bedId)는 병동 소유 데이터라 저장하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdmissionReplyHandler {

    private final DispositionService dispositionService;
    private final ObjectMapper objectMapper;

    /** @param bedAssigned true 면 병상 배정 완료(02), false 면 입원 거부(03) */
    public void handle(String payload, boolean bedAssigned) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String dispositionId = node.path("dispositionId").asText("");
            if (dispositionId.isBlank()) {
                log.warn("병동 회신에 dispositionId 가 없어 무시합니다: {}", payload);
                return;
            }
            String status = bedAssigned ? EmgCodes.ADMISSION_BED_ASSIGNED : EmgCodes.ADMISSION_REJECTED;
            // admissionRequestId 는 선택: 있으면 그 요청에, 없으면 가장 최근 요청에 반영한다
            String requestId = node.path("admissionRequestId").asText("");
            String wardCode = bedAssigned ? node.path("wardCode").asText("") : "";
            dispositionService.updateAdmissionStatus(dispositionId, requestId.isBlank() ? null : requestId, status,
                    wardCode.isBlank() ? null : wardCode);
            log.info("병동 회신 반영 dispositionId={} status={}", dispositionId, status);
        } catch (Exception e) {
            // 잘못된 메시지 하나가 리스너를 멈추지 않게 로그만 남긴다
            log.error("병동 회신 처리 실패: {} ({})", payload, e.getMessage());
        }
    }
}
