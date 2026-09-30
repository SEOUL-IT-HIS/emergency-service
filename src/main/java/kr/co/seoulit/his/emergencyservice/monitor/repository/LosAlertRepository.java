package kr.co.seoulit.his.emergencyservice.monitor.repository;

import kr.co.seoulit.his.emergencyservice.monitor.entity.LosAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LosAlertRepository extends JpaRepository<LosAlert, String> {
    List<LosAlert> findByAcknowledgedAtIsNull();

    // 이 기준시간으로 이미 알림을 만든 접수ID 목록 — 같은 기준시간으로는 접수 건당 한 번만 알린다(확인 후 다시 울리지 않게).
    // 접수마다 exists 로 묻지 않고(N+1) 한 번에 가져와 메모리에서 확인한다.
    @Query("SELECT DISTINCT a.receptionId FROM LosAlert a WHERE a.thresholdMinutes = :thresholdMinutes")
    List<String> findReceptionIdsByThresholdMinutes(@Param("thresholdMinutes") Integer thresholdMinutes);
}
