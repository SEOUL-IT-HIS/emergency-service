package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, String> {
    boolean existsByReceptionId(String receptionId);

    List<ClinicalNote> findByReceptionId(String receptionId);
    List<ClinicalNote> findByReceptionIdOrderByRecordedAtAsc(String receptionId);
}
