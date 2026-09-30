package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DispositionRepository extends JpaRepository<Disposition, String> {

    @Query("SELECT DISTINCT d.receptionId FROM Disposition d WHERE d.receptionId IS NOT NULL")
    List<String> findDistinctReceptionIds();

    List<Disposition> findByReceptionIdOrderByDecidedAtDesc(String receptionId);
}
