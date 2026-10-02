package kr.co.seoulit.his.emergencyservice.care.messaging;


import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionIntakeCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ReceptionIntakeDto;
import kr.co.seoulit.his.emergencyservice.care.messaging.dto.ReceptionIntakeEvent;
import kr.co.seoulit.his.emergencyservice.care.service.CareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceptionIntakeEventService {

    private final CareService careService;

    public void handle(ReceptionIntakeEvent event){
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

        return dto;
    }

}
