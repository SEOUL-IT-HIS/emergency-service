package kr.co.seoulit.his.emergencyservice.channel.repository;

import kr.co.seoulit.his.emergencyservice.channel.entity.SurgeryRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurgeryRequestRepository extends JpaRepository<SurgeryRequest, Long> {
}
