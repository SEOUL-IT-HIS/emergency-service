package kr.co.seoulit.his.emergencyservice.care.service;

import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordCreateRequestDto;
import kr.co.seoulit.his.emergencyservice.care.dto.ConsentRecordDto;
import kr.co.seoulit.his.emergencyservice.care.entity.ConsentRecord;
import kr.co.seoulit.his.emergencyservice.care.repository.ConsentRecordRepository;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeResolver;
import kr.co.seoulit.his.emergencyservice.commoncode.EmgCodes;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsentServiceImplTest {

    private ConsentRecordRepository repository;
    private CommonCodeCache commonCodeCache;
    private ConsentServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ConsentRecordRepository.class);
        when(repository.save(any(ConsentRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        commonCodeCache = new CommonCodeCache();
        service = new ConsentServiceImpl(repository, new CommonCodeResolver(commonCodeCache));
    }

    private static ConsentRecordCreateRequestDto valid() {
        ConsentRecordCreateRequestDto r = new ConsentRecordCreateRequestDto();
        r.setEncounterId("test-reception-001");
        r.setConsentTypeCode(EmgCodes.CONSENT_TYPE_ALLOWED.get(0));
        r.setConsentStatusCode(EmgCodes.CONSENT_STATUS_AGREED);
        r.setConsentedByCode(EmgCodes.CONSENT_BY_SELF);
        r.setRecordedById("nurse-001");
        return r;
    }

    private static AdminCommonCodeItemDto code(String value, String useYn) {
        AdminCommonCodeItemDto c = new AdminCommonCodeItemDto();
        c.setCodeValue(value);
        c.setUseYn(useYn);
        return c;
    }

    @Test
    void recordsAgreedConsentWithReceivedAtDefaultingToNow() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        ConsentRecordDto dto = service.createConsent(valid());

        assertThat(dto.getReceptionId()).isEqualTo("test-reception-001");
        assertThat(dto.getConsentTypeCode()).isEqualTo(EmgCodes.CONSENT_TYPE_ALLOWED.get(0));
        assertThat(dto.getConsentStatusCode()).isEqualTo(EmgCodes.CONSENT_STATUS_AGREED);
        assertThat(dto.getConsentedByCode()).isEqualTo(EmgCodes.CONSENT_BY_SELF);
        assertThat(dto.getConsenterName()).isNull();
        assertThat(dto.getReason()).isNull();
        assertThat(dto.getReceivedAt()).isAfterOrEqualTo(before);
        assertThat(dto.getRecordedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void keepsGivenReceivedAtAndTrimsTexts() {
        ConsentRecordCreateRequestDto r = valid();
        LocalDateTime received = LocalDateTime.now().minusHours(2);
        r.setReceivedAt(received);
        r.setConsentedByCode(EmgCodes.CONSENT_BY_GUARDIAN);
        r.setConsenterName("  Hong Gil-dong  ");
        r.setConsentStatusCode(EmgCodes.CONSENT_STATUS_REFUSED);
        r.setReason("  refused by family  ");

        ConsentRecordDto dto = service.createConsent(r);

        assertThat(dto.getReceivedAt()).isEqualTo(received);
        assertThat(dto.getConsenterName()).isEqualTo("Hong Gil-dong");
        assertThat(dto.getReason()).isEqualTo("refused by family");
    }

    @Test
    void rejectsMissingRequiredFields() {
        for (String field : List.of("encounterId", "consentTypeCode", "consentStatusCode", "consentedByCode", "recordedById")) {
            ConsentRecordCreateRequestDto r = valid();
            switch (field) {
                case "encounterId" -> r.setEncounterId(" ");
                case "consentTypeCode" -> r.setConsentTypeCode(null);
                case "consentStatusCode" -> r.setConsentStatusCode("");
                case "consentedByCode" -> r.setConsentedByCode(null);
                default -> r.setRecordedById(" ");
            }
            assertThatThrownBy(() -> service.createConsent(r))
                    .as(field)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("required");
        }
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsValuesOutsideTheCodeSets() {
        ConsentRecordCreateRequestDto badType = valid();
        badType.setConsentTypeCode("03");
        assertThatThrownBy(() -> service.createConsent(badType)).hasMessageContaining("consentTypeCode");

        ConsentRecordCreateRequestDto badStatus = valid();
        badStatus.setConsentStatusCode("99");
        assertThatThrownBy(() -> service.createConsent(badStatus)).hasMessageContaining("consentStatusCode");

        ConsentRecordCreateRequestDto badBy = valid();
        badBy.setConsentedByCode("99");
        assertThatThrownBy(() -> service.createConsent(badBy)).hasMessageContaining("consentedByCode");
    }

    @Test
    void deferredNeedsReason() {
        ConsentRecordCreateRequestDto r = valid();
        r.setConsentStatusCode(EmgCodes.CONSENT_STATUS_DEFERRED);

        assertThatThrownBy(() -> service.createConsent(r)).hasMessageContaining("reason is required");

        r.setReason("patient unconscious");
        assertThat(service.createConsent(r).getConsentStatusCode()).isEqualTo(EmgCodes.CONSENT_STATUS_DEFERRED);
    }

    @Test
    void guardianNeedsName() {
        ConsentRecordCreateRequestDto r = valid();
        r.setConsentedByCode(EmgCodes.CONSENT_BY_GUARDIAN);

        assertThatThrownBy(() -> service.createConsent(r)).hasMessageContaining("consenterName is required");

        r.setConsenterName("Hong");
        assertThat(service.createConsent(r).getConsentedByCode()).isEqualTo(EmgCodes.CONSENT_BY_GUARDIAN);
    }

    @Test
    void rejectsFutureReceivedAtButAllowsSmallClockSkew() {
        ConsentRecordCreateRequestDto future = valid();
        future.setReceivedAt(LocalDateTime.now().plusHours(1));
        assertThatThrownBy(() -> service.createConsent(future)).hasMessageContaining("future");

        ConsentRecordCreateRequestDto skew = valid();
        skew.setReceivedAt(LocalDateTime.now().plusMinutes(2));
        assertThat(service.createConsent(skew)).isNotNull();
    }

    @Test
    void rejectsTooLongTexts() {
        ConsentRecordCreateRequestDto longName = valid();
        longName.setConsenterName("a".repeat(101));
        assertThatThrownBy(() -> service.createConsent(longName)).hasMessageContaining("consenterName");

        ConsentRecordCreateRequestDto longReason = valid();
        longReason.setReason("a".repeat(501));
        assertThatThrownBy(() -> service.createConsent(longReason)).hasMessageContaining("reason");
    }

    @Test
    void adminCodesTakePrecedenceButOnlyTheAllowedConsentTypesAreUsable() {
        // admin: 01 사용, 05 비활성, 03(비용견적)은 admin에만 있고 응급이 안 쓰는 값
        commonCodeCache.put("CONSENT_TYPE_CD", List.of(code("01", "Y"), code("05", "N"), code("03", "Y")));

        ConsentRecordCreateRequestDto ok = valid();
        assertThat(service.createConsent(ok).getConsentTypeCode()).isEqualTo("01");

        ConsentRecordCreateRequestDto inactive = valid();
        inactive.setConsentTypeCode("05");
        assertThatThrownBy(() -> service.createConsent(inactive)).hasMessageContaining("consentTypeCode");

        ConsentRecordCreateRequestDto notAllowed = valid();
        notAllowed.setConsentTypeCode("03");
        assertThatThrownBy(() -> service.createConsent(notAllowed)).hasMessageContaining("consentTypeCode");

        ConsentRecordCreateRequestDto notInAdmin = valid();
        notInAdmin.setConsentTypeCode("02");
        assertThatThrownBy(() -> service.createConsent(notInAdmin)).hasMessageContaining("consentTypeCode");
    }

    @Test
    void listRequiresReceptionIdAndMapsRows() {
        assertThatThrownBy(() -> service.getConsents(" ")).isInstanceOf(IllegalArgumentException.class);

        ConsentRecord row = new ConsentRecord();
        row.setId("c-1");
        row.setReceptionId("test-reception-001");
        row.setConsentTypeCode("05");
        row.setConsentStatusCode(EmgCodes.CONSENT_STATUS_AGREED);
        row.setConsentedByCode(EmgCodes.CONSENT_BY_SELF);
        row.setReceivedAt(LocalDateTime.of(2026, 9, 30, 9, 0));
        row.setRecordedById("nurse-001");
        row.setRecordedAt(LocalDateTime.of(2026, 9, 30, 9, 1));
        when(repository.findByReceptionIdOrderByReceivedAtDescRecordedAtDesc("test-reception-001"))
                .thenReturn(List.of(row));

        List<ConsentRecordDto> list = service.getConsents("test-reception-001");

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo("c-1");
        assertThat(list.get(0).getConsentTypeCode()).isEqualTo("05");
    }
}
