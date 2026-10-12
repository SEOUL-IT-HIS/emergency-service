package kr.co.seoulit.his.emergencyservice.disposition.messaging;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;

/** 오접수·중복 접수로 닫은 사실을 접수 서비스에 알린다. app.kafka.void.enabled=true 일 때만 실제 Kafka 로 보낸다. */
public interface ReceptionVoidPublisher {

    /** @param disposition 유형 06(오접수·중복 접수)으로 결정한 퇴실 결정. 처음 결정할 때와 재전송할 때 모두 쓴다 */
    void publishVoided(Disposition disposition);
}
