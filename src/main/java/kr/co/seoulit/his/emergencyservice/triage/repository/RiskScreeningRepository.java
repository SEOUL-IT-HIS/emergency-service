package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.RiskScreening;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RiskScreeningRepository extends JpaRepository<RiskScreening, String> {
    List<RiskScreening> findByReceptionId(String receptionId);
}
