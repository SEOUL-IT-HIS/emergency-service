package kr.co.seoulit.his.emergencyservice.monitor.repository;

import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LosAlertRepository extends JpaRepository<LosAlert, String> {
    List<LosAlert> findByAcknowledgedAtIsNull();
}
