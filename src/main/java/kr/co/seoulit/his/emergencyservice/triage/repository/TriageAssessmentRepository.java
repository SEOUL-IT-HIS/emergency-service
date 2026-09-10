package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TriageAssessmentRepository extends JpaRepository<TriageAssessment, String> {
    List<TriageAssessment> findByReceptionId(String receptionId);
    List<TriageAssessment> findByReceptionIdOrderByAssessedAtAsc(String receptionId);
    boolean existsByReceptionIdAndAssessmentTypeCode(String receptionId, String assessmentTypeCode);
}
