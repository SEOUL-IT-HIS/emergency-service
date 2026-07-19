package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.IsolationAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IsolationAssessmentRepository extends JpaRepository<IsolationAssessment, Long> {
    List<IsolationAssessment> findByReceptionNo(String receptionNo);
}
