package kr.co.seoulit.his.emergencyservice.channel.repository;

import kr.co.seoulit.his.emergencyservice.channel.entity.SurgeryOrderStatusRef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurgeryOrderStatusRefRepository extends JpaRepository<SurgeryOrderStatusRef, String> {
    Optional<SurgeryOrderStatusRef> findByOrderId(String orderId);
}
