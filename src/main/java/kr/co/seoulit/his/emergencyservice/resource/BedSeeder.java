package kr.co.seoulit.his.emergencyservice.resource;

import kr.co.seoulit.his.emergencyservice.resource.entity.Bed;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedRepository;
import lombok.RequiredArgsConstructor;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 병상 마스터 목데이터 시더.
 * 구역(zoneCode)은 docs/models.md 4장 ZONE 코드 그룹을 따르고,
 * 병상유형(bedTypeCode)은 일반/처치/격리 3종으로 구분한다.
 * RCP/실제 시설 데이터 연동 전까지 임시로 사용.
 */
@Component
@RequiredArgsConstructor
public class BedSeeder implements CommandLineRunner {

    private final BedRepository bedRepository;

    @Override
    public void run(String... args) {
        if (bedRepository.count() > 0) {
            return;
        }
        save("C-01", EmgCodes.ZONE_CRITICAL, EmgCodes.BED_TYPE_GENERAL);
        save("U-01", EmgCodes.ZONE_URGENT, EmgCodes.BED_TYPE_GENERAL);
        save("U-02", EmgCodes.ZONE_URGENT, EmgCodes.BED_TYPE_TREATMENT);
        save("I-01", EmgCodes.ZONE_ISOLATION, EmgCodes.BED_TYPE_ISOLATION_ROOM);
        save("I-02", EmgCodes.ZONE_ISOLATION, EmgCodes.BED_TYPE_ISOLATION_ROOM);
        save("I-03", EmgCodes.ZONE_ISOLATION, EmgCodes.BED_TYPE_ISOLATION_ROOM);
        save("I-04", EmgCodes.ZONE_ISOLATION, EmgCodes.BED_TYPE_ISOLATION_ROOM);
    }

    private void save(String bedNo, String zoneCode, String bedTypeCode) {
        Bed bed = new Bed();
        bed.setBedNo(bedNo);
        bed.setZoneCode(zoneCode);
        bed.setBedTypeCode(bedTypeCode);
        bed.setBedStatusCode(EmgCodes.BED_STATUS_EMPTY);
        bed.setCreatedAt(LocalDateTime.now());
        bed.setUpdatedAt(LocalDateTime.now());
        bedRepository.save(bed);
    }
}
