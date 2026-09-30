package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispositionRepository extends JpaRepository<Disposition, String> {

    List<Disposition> findByReceptionIdIn(java.util.Collection<String> receptionIds);

    List<Disposition> findByReceptionIdOrderByDecidedAtDesc(String receptionId);
}
