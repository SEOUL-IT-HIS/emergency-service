package kr.co.seoulit.his.emergencyservice.resource.repository;

import kr.co.seoulit.his.emergencyservice.resource.entity.BedAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BedAssignmentRepository extends JpaRepository<BedAssignment, Long> {
    List<BedAssignment> findByReceptionNoAndReleasedAtIsNull(String receptionNo);
}
