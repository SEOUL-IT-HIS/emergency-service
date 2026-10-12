package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.Disposition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispositionRepository extends JpaRepository<Disposition, String> {
    boolean existsByReceptionId(String receptionId);


    List<Disposition> findByReceptionIdIn(java.util.Collection<String> receptionIds);

    List<Disposition> findByReceptionIdOrderByDecidedAtDesc(String receptionId);

    /** 어떤 유형으로 결정한 것 중 기준 시각 이후의 결정 — 접수 서비스로 다시 보낼 오접수 건을 찾는 데 쓴다 */
    List<Disposition> findByDispositionTypeCodeAndDecidedAtAfter(String dispositionTypeCode, java.time.LocalDateTime after);
}
