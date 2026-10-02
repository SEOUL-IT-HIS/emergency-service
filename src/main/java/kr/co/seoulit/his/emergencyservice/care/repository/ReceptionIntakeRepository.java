package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReceptionIntakeRepository extends JpaRepository<ReceptionIntake, String> {

    List<ReceptionIntake> findByPatientId(String patientId);
}
