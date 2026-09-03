package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.EmsReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmsReferralRepository extends JpaRepository<EmsReferral, String> {
    List<EmsReferral> findByReceptionId(String receptionId);
}
