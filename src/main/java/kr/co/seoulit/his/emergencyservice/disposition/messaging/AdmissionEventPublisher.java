package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;

/** 입원요청 이벤트 발행. app.kafka.admission.enabled=true 일 때만 실제 Kafka 로 보낸다. */
public interface AdmissionEventPublisher {

    /** @param wardPref 희망 병동(WARD_CD 값, 없으면 null) */
    void publishRequested(Disposition disposition, AdmissionRequest request, String wardPref);
}
