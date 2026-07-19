package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.TransferNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferNoteRepository extends JpaRepository<TransferNote, Long> {
}
