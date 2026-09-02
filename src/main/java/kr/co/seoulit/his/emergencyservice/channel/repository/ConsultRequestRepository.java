package kr.co.seoulit.his.emergencyservice.channel.repository;

import kr.co.seoulit.his.emergencyservice.channel.entity.ConsultRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultRequestRepository extends JpaRepository<ConsultRequest, String> {
}
