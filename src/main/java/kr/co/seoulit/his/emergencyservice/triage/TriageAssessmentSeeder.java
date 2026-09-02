package kr.co.seoulit.his.emergencyservice.triage;

import kr.co.seoulit.his.emergencyservice.triage.entity.TriageAssessment;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * UC-CARE-01 — 접수(RCP)가 아직 응급접수 생성 API를 제공하지 않는 동안,
 * 응급환자 목록(GET /api/emergency/care/patients)이 비어있지 않도록 채우는 로컬 개발용 시드.
 *
 * CareServiceImpl.getPatients()는 RCP 호출 없이 TRIAGE_ASSESSMENT만 읽으므로,
 * 이 테이블에 최초 분류(INITIAL) 레코드가 있으면 목록에 바로 노출된다.
 * EmsReferralSeeder와 동일한 receptionNo(ER-20260716-001/002)를 재사용해 EMS 패널과
 * 환자가 일치하도록 맞췄고, 마지막 한 건은 EMS 이송 없이 내원한 케이스로 추가했다.
 * 애플리케이션 기동 시 테이블이 비어 있을 때만 채우는 멱등 시드다.
 */
@Component
@RequiredArgsConstructor
public class TriageAssessmentSeeder implements CommandLineRunner {

    private final TriageAssessmentRepository triageAssessmentRepository;

    @Override
    public void run(String... args) {
        if (triageAssessmentRepository.count() > 0) {
            return;
        }

        save("ER-20260716-001", "2", "흉통, EMS 이송(활력징후 불안정)", LocalDateTime.now().minusMinutes(12));
        save("ER-20260716-002", "4", "경미한 열상, EMS 이송(활력징후 안정)", LocalDateTime.now().minusMinutes(30));
        save("ER-20260716-003", "1", "호흡곤란, 보호자 동반 내원(EMS 미이송)", LocalDateTime.now().minusMinutes(5));
    }

    private void save(String receptionNo, String ktasLevelCode, String reason, LocalDateTime assessedAt) {
        TriageAssessment assessment = new TriageAssessment();
        assessment.setReceptionNo(receptionNo);
        assessment.setKtasLevelCode(ktasLevelCode);
        assessment.setAssessmentTypeCode("INITIAL");
        assessment.setAssessedById("E0001");
        assessment.setAssessedAt(assessedAt);
        assessment.setReason(reason);
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());
        triageAssessmentRepository.save(assessment);
    }
}
