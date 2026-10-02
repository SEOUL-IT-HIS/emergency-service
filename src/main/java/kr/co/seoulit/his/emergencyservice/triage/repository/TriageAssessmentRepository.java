package kr.co.seoulit.his.emergencyservice.triage.repository;

import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface TriageAssessmentRepository extends JpaRepository<TriageAssessment, String> {
    List<TriageAssessment> findByReceptionId(String receptionId);
    List<TriageAssessment> findByReceptionIdOrderByAssessedAtAsc(String receptionId);
    // 여러 접수의 KTAS 이력을 한 번에(오래된 순) — 환자 목록 N+1 방지
    List<TriageAssessment> findByReceptionIdInOrderByAssessedAtAsc(Collection<String> receptionIds);
    boolean existsByReceptionIdAndAssessmentTypeCode(String receptionId, String assessmentTypeCode);
}
