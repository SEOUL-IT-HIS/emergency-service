package kr.co.seoulit.his.emergencyservice.triage;

import kr.co.seoulit.his.emergencyservice.triage.entity.EmsReferral;
import kr.co.seoulit.his.emergencyservice.triage.repository.EmsReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * UC-TRI-01 / Jira UD2-46 — EMS(119) 실연동 없이 "연동된 것처럼" 보이기 위한 Mock 시드.
 *
 * 애플리케이션 기동 시 EMS_REFERRAL 테이블이 비어 있으면 샘플 이송정보를 채운다.
 * 실제 외부망 클라이언트는 만들지 않으며(요구사항 명시), 이 시드는 멱등(idempotent)하다 —
 * 이미 데이터가 있으면 아무 것도 하지 않으므로 반복 기동해도 중복 삽입되지 않는다.
 * receptionNo 는 다른 Triage 하위 기능(KTAS/EWS/격리/스크리닝) 데모와 맞춰 쓸 수 있도록
 * "ER-20260716-001", "ER-20260716-002" 를 사용한다.
 */
@Component
@RequiredArgsConstructor
public class EmsReferralSeeder implements CommandLineRunner {

    private final EmsReferralRepository emsReferralRepository;

    @Override
    public void run(String... args) {
        if (emsReferralRepository.count() > 0) {
            return;
        }

        EmsReferral first = new EmsReferral();
        first.setReceptionNo("ER-20260716-001");
        first.setEmsAgencyName("서울소방재난본부 119구급대");
        first.setVitalsOnScene("BP 90/60, HR 120, RR 24, SpO2 92%, GCS 13");
        first.setPrehospitalTreatment("산소투여 5L/min, 정맥로 확보, 생리식염수 500ml 투여");
        first.setTransmittedAt(LocalDateTime.now().minusMinutes(15));
        first.setCreatedAt(LocalDateTime.now());
        first.setUpdatedAt(LocalDateTime.now());
        emsReferralRepository.save(first);

        EmsReferral second = new EmsReferral();
        second.setReceptionNo("ER-20260716-002");
        second.setEmsAgencyName("경기도소방재난본부 119구급대");
        second.setVitalsOnScene("BP 130/85, HR 88, RR 18, SpO2 98%, GCS 15");
        second.setPrehospitalTreatment("특이 처치 없음, 활력징후 안정");
        second.setTransmittedAt(LocalDateTime.now().minusMinutes(40));
        second.setCreatedAt(LocalDateTime.now());
        second.setUpdatedAt(LocalDateTime.now());
        emsReferralRepository.save(second);
    }
}
