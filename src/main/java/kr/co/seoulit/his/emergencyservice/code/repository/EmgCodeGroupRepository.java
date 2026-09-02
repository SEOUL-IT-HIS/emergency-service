package kr.co.seoulit.his.emergencyservice.code.repository;

import kr.co.seoulit.his.emergencyservice.code.entity.EmgCodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmgCodeGroupRepository extends JpaRepository<EmgCodeGroup, String> {
    Optional<EmgCodeGroup> findByGroupCode(String groupCode);
}
