package kr.co.seoulit.his.emergencyservice.disposition.repository;

import kr.co.seoulit.his.emergencyservice.disposition.entity.AdmissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdmissionRequestRepository extends JpaRepository<AdmissionRequest, String> {
    java.util.List<AdmissionRequest> findByDispositionIdOrderByRequestedAtDesc(String dispositionId);
    java.util.List<AdmissionRequest> findByDispositionIdIn(java.util.Collection<String> dispositionIds);
}
