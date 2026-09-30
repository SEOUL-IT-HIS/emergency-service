package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.*;
import kr.co.seoulit.his.emergencyservice.care.entity.CprEvent;
import kr.co.seoulit.his.emergencyservice.care.entity.CprTimeline;
import kr.co.seoulit.his.emergencyservice.care.mapper.CareMapstructMapper;
import kr.co.seoulit.his.emergencyservice.care.repository.*;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.patient.client.PatientClient;
import kr.co.seoulit.his.emergencyservice.resource.repository.BedAssignmentRepository;
import kr.co.seoulit.his.emergencyservice.triage.repository.TriageAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 처치·투약·CPR 기록의 코드값 검증과 조회(GET) 변환 */
class CareServiceImplCodeTest {

    private CprEventRepository cprEventRepository;
    private CareServiceImpl service;

    @BeforeEach
    void setUp() {
        cprEventRepository = mock(CprEventRepository.class);
        CommonCodeCache cache = new CommonCodeCache();
        service = new CareServiceImpl(
                mock(ClinicalNoteRepository.class),
                mock(TreatmentRecordRepository.class),
                mock(MedicationAdministrationRepository.class),
                cprEventRepository,
                mock(TriageAssessmentRepository.class),
                mock(BedAssignmentRepository.class),
                mock(ReceptionIntakeRepository.class),
                cache,
                new CommonCodeResolver(cache),
                mock(CareMapstructMapper.class),
                mock(PatientClient.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.DispositionRepository.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.AdmissionRequestRepository.class),
                mock(kr.co.seoulit.his.emergencyservice.disposition.repository.TransferNoteRepository.class));
    }

    @Test
    void treatmentCodeMustBeOneOfTheTreatmentTypes() {
        TreatmentCreateRequestDto r = new TreatmentCreateRequestDto();
        r.setEncounterId("rc-1");
        r.setOrderId("order-uuid");
        r.setTreatmentCode("99");
        r.setPerformedById("nurse-1");

        assertThatThrownBy(() -> service.createTreatment(r))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("treatmentCode must be one of 01");
    }

    @Test
    void administrationRouteMustBeOneOfTheAdminRoutes() {
        MarCreateRequestDto r = new MarCreateRequestDto();
        r.setEncounterId("rc-1");
        r.setOrderId("order-uuid");
        r.setDrugCode("D1");
        r.setDose("1 amp");
        r.setRouteCode("IV");
        r.setAdministeredById("nurse-1");
        r.setAdministeredAt(LocalDateTime.now());

        assertThatThrownBy(() -> service.createMar(r))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("routeCode must be one of");
    }

    @Test
    void cprEventTypeAndOutcomeAreValidated() {
        CprTimelineCreateRequestDto.CprEventItemDto item = new CprTimelineCreateRequestDto.CprEventItemDto();
        item.setEventTypeCode("COMPRESSION");
        item.setRecordedById("nurse-1");
        CprTimelineCreateRequestDto badType = new CprTimelineCreateRequestDto();
        badType.setEncounterId("rc-1");
        badType.setEvents(List.of(item));
        assertThatThrownBy(() -> service.createCprTimeline(badType)).hasMessageContaining("eventTypeCode");

        item.setEventTypeCode("01");
        badType.setOutcomeCode("99");
        assertThatThrownBy(() -> service.createCprTimeline(badType)).hasMessageContaining("outcomeCode");
    }

    @Test
    void listApisRequireReceptionId() {
        assertThatThrownBy(() -> service.getTreatments(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getMars(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getCprEvents("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cprEventsAreReturnedWithTimelinesInEventTimeOrder() {
        CprEvent event = new CprEvent();
        event.setId("e-1");
        event.setReceptionId("rc-1");
        event.setStartedAt(LocalDateTime.of(2026, 9, 30, 10, 0));
        event.setOutcomeCode("01");
        event.setTimelines(new ArrayList<>(List.of(
                timeline("t-2", LocalDateTime.of(2026, 9, 30, 10, 5), "02"),
                timeline("t-1", LocalDateTime.of(2026, 9, 30, 10, 1), "01"))));
        when(cprEventRepository.findByReceptionIdOrderByStartedAtDesc("rc-1")).thenReturn(List.of(event));

        List<CprEventDto> result = service.getCprEvents("rc-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTimelines()).extracting(CprEventDto.CprTimelineItemDto::getId)
                .containsExactly("t-1", "t-2");
    }

    private static CprTimeline timeline(String id, LocalDateTime at, String type) {
        CprTimeline t = new CprTimeline();
        t.setId(id);
        t.setEventAt(at);
        t.setEventTypeCode(type);
        return t;
    }
}
