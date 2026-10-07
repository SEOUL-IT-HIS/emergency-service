package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, String> {
    boolean existsByReceptionId(String receptionId);

    List<ConsentRecord> findByReceptionIdOrderByReceivedAtDescRecordedAtDesc(String receptionId);
}
