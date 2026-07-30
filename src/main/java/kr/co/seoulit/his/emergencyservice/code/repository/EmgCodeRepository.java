package kr.co.seoulit.his.emergencyservice.code.repository;

import kr.co.seoulit.his.emergencyservice.code.entity.EmgCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmgCodeRepository extends JpaRepository<EmgCode, String> {
    List<EmgCode> findByCodeGroup_GroupCodeOrderBySortOrderAsc(String groupCode);
}
