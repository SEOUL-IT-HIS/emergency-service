package kr.co.seoulit.his.emergencyservice.channel.repository;

import kr.co.seoulit.his.emergencyservice.channel.entity.OncallRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OncallRequestRepository extends JpaRepository<OncallRequest, String> {
}
