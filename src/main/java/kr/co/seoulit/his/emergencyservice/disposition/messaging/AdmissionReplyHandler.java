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
 * 응급은 병동 병상 정보(wardCode, bedId)를 저장하지 않고 상태만 반영한다(병동 소유 데이터).
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
            dispositionService.updateAdmissionStatus(dispositionId, status);
            log.info("병동 회신 반영 dispositionId={} status={}", dispositionId, status);
        } catch (Exception e) {
            // 잘못된 메시지 하나가 리스너를 멈추지 않게 로그만 남긴다
            log.error("병동 회신 처리 실패: {} ({})", payload, e.getMessage());
        }
    }
}
