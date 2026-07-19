package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.TreatmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TreatmentRecordRepository extends JpaRepository<TreatmentRecord, Long> {
    List<TreatmentRecord> findByReceptionNo(String receptionNo);
}
