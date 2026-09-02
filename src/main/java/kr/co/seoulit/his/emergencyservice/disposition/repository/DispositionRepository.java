package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispositionRepository extends JpaRepository<Disposition, String> {
}
