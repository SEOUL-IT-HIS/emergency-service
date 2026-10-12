package kr.co.seoulit.his.emergencyservice.disposition;

import kr.co.seoulit.his.emergencyservice.care.entity.ReceptionIntake;
import kr.co.seoulit.his.emergencyservice.care.repository.ReceptionIntakeRepository;
import kr.co.seoulit.his.emergencyservice.care.service.ReceptionCancellationService;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.common.exception.ConflictException;
import kr.co.seoulit.his.emergencyservice.disposition.dto.DispositionCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.disposition.dto.DispositionDto;
import kr.co.seoulit.his.emergencyservice.disposition.service.DischargeProgress;
import kr.co.seoulit.his.emergencyservice.disposition.service.DispositionService;
import kr.co.seoulit.his.emergencyservice.resource.dto.BedAssignmentCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.resource.dto.BedAssignmentDto;
import kr.co.seoulit.his.emergencyservice.resource.entity.Bed;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedRepository;
import kr.co.seoulit.his.emergencyservice.resource.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 진료 도중(병상이 배정된 접수)에 오접수·중복 접수(06)로 닫으면 병상이 실제로 반납되는지 — 메모리 DB(H2)로 서비스 전체를 띄워 확인한다.
 * 공용 DB·Kafka·접수 서비스는 건드리지 않는다(Kafka·오접수 발행은 끈다).
 */
@SpringBootTest
class VoidDispositionBedReleaseIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:emgvoid;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true;INIT=CREATE SCHEMA IF NOT EXISTS EMERGENCY");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.default_schema", () -> "EMERGENCY");
        registry.add("app.kafka.intake.enabled", () -> "false");
        registry.add("app.kafka.admission.enabled", () -> "false");
        registry.add("app.kafka.void.enabled", () -> "false");
    }

    @Autowired
    private ReceptionIntakeRepository receptionIntakeRepository;
    @Autowired
    private BedRepository bedRepository;
    @Autowired
    private BedAssignmentRepository bedAssignmentRepository;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private DispositionService dispositionService;
    @Autowired
    private DischargeProgress dischargeProgress;
    @Autowired
    private ReceptionCancellationService cancellationService;

    private String newReception() {
        ReceptionIntake intake = new ReceptionIntake();
        intake.setId(UUID.randomUUID().toString());
        intake.setPatientId(UUID.randomUUID().toString());
        intake.setReceivedAt(LocalDateTime.now().minusMinutes(30));
        intake.setCreatedAt(LocalDateTime.now());
        intake.setUpdatedAt(LocalDateTime.now());
        return receptionIntakeRepository.save(intake).getId();
    }

    private static DispositionCreateRequestDto voidRequest(String receptionId) {
        DispositionCreateRequestDto request = new DispositionCreateRequestDto();
        request.setEncounterId(receptionId);
        request.setDispositionType(EmgCodes.DISPOSITION_VOID);
        request.setDecidedById("doctor-1");
        request.setVoidReasonType(EmgCodes.VOID_REASON_DUPLICATE);
        request.setVoidReason("같은 환자를 두 번 접수함");
        return request;
    }

    @Test
    void 병상이_배정된_접수를_06으로_닫으면_병상이_반납되고_퇴실_완료가_된다() {
        String receptionId = newReception();
        Bed bed = bedRepository.findAll().stream()
                .filter(b -> EmgCodes.BED_STATUS_EMPTY.equals(b.getBedStatusCode())).findFirst().orElseThrow();
        BedAssignmentCreateRequestDto assign = new BedAssignmentCreateRequestDto();
        assign.setEncounterId(receptionId);
        assign.setBedId(bed.getId());
        assign.setAssignedById("nurse-1");
        BedAssignmentDto assigned = resourceService.assignBed(assign);
        assertThat(assigned).isNotNull();
        assertThat(bedAssignmentRepository.findActiveWithBedByReceptionId(receptionId)).hasSize(1);
        assertThat(bedRepository.findById(bed.getId()).orElseThrow().getBedStatusCode()).isNotEqualTo(EmgCodes.BED_STATUS_EMPTY);

        DispositionDto decided = dispositionService.createDisposition(voidRequest(receptionId));

        assertThat(decided.getDispositionTypeCode()).isEqualTo(EmgCodes.DISPOSITION_VOID);
        // 06 은 퇴실 완료와 같이 처리되어 병상이 비고, 접수는 퇴실 단계 DONE 이 된다
        assertThat(bedAssignmentRepository.findActiveWithBedByReceptionId(receptionId)).isEmpty();
        assertThat(bedRepository.findById(bed.getId()).orElseThrow().getBedStatusCode()).isEqualTo(EmgCodes.BED_STATUS_EMPTY);
        assertThat(dischargeProgress.stage(receptionId)).isEqualTo(DischargeProgress.Stage.DONE);
    }

    @Test
    void 응급이_06으로_닫은_접수는_접수의_취소를_받아들인다() {
        String receptionId = newReception();
        dispositionService.createDisposition(voidRequest(receptionId));

        assertThat(cancellationService.check(receptionId).isCancellable()).isTrue();
    }

    @Test
    void 이미_06으로_닫은_접수에_병상을_다시_배정할_수_없다() {
        String receptionId = newReception();
        Bed bed = bedRepository.findAll().stream()
                .filter(b -> EmgCodes.BED_STATUS_EMPTY.equals(b.getBedStatusCode())).findFirst().orElseThrow();
        dispositionService.createDisposition(voidRequest(receptionId));

        BedAssignmentCreateRequestDto assign = new BedAssignmentCreateRequestDto();
        assign.setEncounterId(receptionId);
        assign.setBedId(bed.getId());
        assign.setAssignedById("nurse-1");

        assertThatThrownBy(() -> resourceService.assignBed(assign)).isInstanceOf(ConflictException.class);
    }
}
