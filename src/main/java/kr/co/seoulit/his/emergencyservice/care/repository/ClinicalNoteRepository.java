package kr.co.seoulit.his.emergencyservice.care.repository;

import kr.co.seoulit.his.emergencyservice.care.entity.ClinicalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClinicalNoteRepository extends JpaRepository<ClinicalNote, Long> {
    List<ClinicalNote> findByReceptionNo(String receptionNo);
}
