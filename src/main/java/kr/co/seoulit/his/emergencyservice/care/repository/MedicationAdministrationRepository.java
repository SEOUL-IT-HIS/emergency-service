package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.MedicationAdministration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministration, Long> {
    List<MedicationAdministration> findByReceptionNo(String receptionNo);
}
