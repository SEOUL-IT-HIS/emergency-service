package kr.co.seoulit.his.emergencyservice.resource.repository;

import kr.co.seoulit.his.emergencyservice.resource.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BedRepository extends JpaRepository<Bed, String> {
    long countByBedStatusCode(String bedStatusCode);
    List<Bed> findByBedStatusCode(String bedStatusCode);
}
