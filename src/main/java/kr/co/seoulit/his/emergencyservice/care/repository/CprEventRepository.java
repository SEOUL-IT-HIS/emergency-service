package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.CprEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CprEventRepository extends JpaRepository<CprEvent, String> {
    boolean existsByReceptionId(String receptionId);

    List<CprEvent> findByReceptionId(String receptionId);

    // 타임라인(LAZY)까지 한 번에 가져온다 — 없으면 CPR 이벤트마다 타임라인 조회가 한 번씩 더 나간다(N+1)
    @EntityGraph(attributePaths = "timelines")
    List<CprEvent> findByReceptionIdOrderByStartedAtDesc(String receptionId);
}
