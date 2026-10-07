package kr.co.seoulit.his.emergencyservice.care.messaging;


import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionIntakeCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionIntakeDto;
import kr.co.seoulit.his.emergencyservice.care.messaging.dto.ReceptionIntakeEvent;
import kr.co.seoulit.his.emergencyservice.care.service.CareService;
import kr.co.seoulit.his.emergencyservice.care.service.ReceptionCancellationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionIntakeEventService {

    private final CareService careService;
    private final ReceptionCancellationService receptionCancellationService;

    public void handle(ReceptionIntakeEvent event){
        if (event.isCancellation()) {
            // 취소가 신규 접수로 저장되면 안 된다 - 등록 경로로 보내지 않고 취소 전용 경로로 처리한다
            try {
                ReceptionCancellationService.Result result =
                        receptionCancellationService.cancel(event.getReceptionId(), event.getOccurredAt());
                log.info("[Kafka] 접수 취소 이벤트 처리 eventId={}, receptionId={}, 결과={}",
                            event.getEventId(), event.getReceptionId(), result);
            } catch (Exception e) {
                log.error("[Kafka] 접수 취소 이벤트 처리 중 예외 eventId={}, receptionId={}",
                            event.getEventId(), event.getReceptionId(), e);
            }
            return;
        }
        try{
            ReceptionIntakeCreateRequestDto request = toRequest(event);
            ReceptionIntakeDto saved = careService.createReceptionIntake(request);
            log.info("[Kafka] 접수 이벤트 처리 성공 eventId={}, receptionId={}",
                        event.getEventId(), saved.getReceptionId());
        } catch (IllegalArgumentException e){
            log.warn("[Kafka] 접수 이벤트 검증 실패 eventId={}, receptionId={}, reason={}",
                        event.getEventId(), event.getReceptionId(), e.getMessage());
        } catch (Exception e){
            log.error("[Kafka] 접수 이벤트 처리 중 예외 eventId={}, receptionId={}",
                        event.getEventId(), event.getReceptionId(), e );
        }
    }

    private ReceptionIntakeCreateRequestDto toRequest(ReceptionIntakeEvent event) {
        ReceptionIntakeCreateRequestDto dto = new ReceptionIntakeCreateRequestDto();
        dto.setReceptionId(event.getReceptionId());
        dto.setPatientId(event.getPatientId());
        dto.setArrivalPath(event.getArrivalPath());
        dto.setReceivedAt(event.getReceivedAt());
        dto.setMemo(event.getMemo());
        dto.setChiefComplaintRaw(event.getChiefComplaintRaw());
        dto.setKtasLevel(event.getKtasLevel());
        dto.setTriageDateTime(event.getTriageDateTime());

        return dto;
    }

}
