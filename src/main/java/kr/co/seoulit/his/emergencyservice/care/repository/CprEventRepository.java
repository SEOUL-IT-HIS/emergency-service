package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.CprEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CprEventRepository extends JpaRepository<CprEvent, String> {
    List<CprEvent> findByReceptionNo(String receptionNo);
}
