package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TriageAssessmentRepository extends JpaRepository<TriageAssessment, String> {
    List<TriageAssessment> findByReceptionNo(String receptionNo);
    List<TriageAssessment> findByReceptionNoOrderByAssessedAtAsc(String receptionNo);
    boolean existsByReceptionNoAndAssessmentTypeCode(String receptionNo, String assessmentTypeCode);
}
