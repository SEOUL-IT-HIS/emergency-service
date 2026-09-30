package kr.co.seoulit.his.emergencyservice.resource.repository;

import kr.co.seoulit.his.emergencyservice.resource.entity.Bed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BedRepository extends JpaRepository<Bed, String> {
    long countByBedStatusCode(String bedStatusCode);
    List<Bed> findByBedStatusCode(String bedStatusCode);

    // IX_BED_ZONE_STATUS 인덱스만으로 처리되는 집계 (대시보드 폴링 대비, INDEX FULL SCAN 확인함)
    @Query("SELECT b.zoneCode AS zoneCode, b.bedStatusCode AS bedStatusCode, COUNT(b) AS bedCount "
            + "FROM Bed b GROUP BY b.zoneCode, b.bedStatusCode")
    List<BedStatusCount> countGroupByZoneAndStatus();
}
