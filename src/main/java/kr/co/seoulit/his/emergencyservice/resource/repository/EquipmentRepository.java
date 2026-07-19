package kr.co.seoulit.his.emergencyservice.resource.repository;

import kr.co.seoulit.his.emergencyservice.resource.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
}
