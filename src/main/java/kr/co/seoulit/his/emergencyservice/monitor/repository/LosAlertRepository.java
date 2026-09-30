package kr.co.seoulit.his.emergencyservice.monitor.repository;

import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LosAlertRepository extends JpaRepository<LosAlert, String> {
    List<LosAlert> findByAcknowledgedAtIsNull();

    // 같은 기준시간으로는 접수 건당 한 번만 알린다 (확인 후 다시 울리지 않게)
    boolean existsByReceptionIdAndThresholdMinutes(String receptionId, Integer thresholdMinutes);
}
