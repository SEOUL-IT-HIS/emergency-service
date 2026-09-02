package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.AmbulanceTransport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AmbulanceTransportRepository extends JpaRepository<AmbulanceTransport, String> {
}
