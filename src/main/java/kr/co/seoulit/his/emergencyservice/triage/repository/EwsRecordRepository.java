package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.EwsRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EwsRecordRepository extends JpaRepository<EwsRecord, String> {
    List<EwsRecord> findByReceptionNo(String receptionNo);
}
