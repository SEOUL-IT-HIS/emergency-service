package kr.co.seoulit.his.emergencyservice.commoncode;

import java.util.List;

/**
 * 응급(EMG)이 쓰는 admin 공통코드 그룹과 값.
 * admin 관례에 맞춰 그룹코드는 끝이 _CD, 코드값은 숫자 2자리다(2026-09-30 결정, 03 작업기록 1-3장).
 * admin 캐시에 그룹이 있으면 admin 값을 쓰고, 없을 때만 아래 FALLBACK 값을 쓴다.
 * 값으로 분기하는 로직(병상 상태 집계, 동의 유예/보호자 등)은 여기 상수를 쓴다.
 */
public final class EmgCodes {

    private EmgCodes() {
    }

    // ---- 그룹코드 ----
    public static final String ZONE_GROUP = "ZONE_CD";
    public static final String BED_STATUS_GROUP = "BED_STATUS_CD";
    public static final String BED_TYPE_GROUP = "BED_TYPE_CD";
    public static final String DISPOSITION_TYPE_GROUP = "DISPOSITION_TYPE_CD";
    /** admin에 이미 있는 그룹(KTAS 01~05) */
    public static final String KTAS_LEVEL_GROUP = "TRIAGE_CD";
    public static final String ASSESSMENT_TYPE_GROUP = "ASSESSMENT_TYPE_CD";
    public static final String ISOLATION_TYPE_GROUP = "ISOLATION_TYPE_CD";
    public static final String SCREENING_TYPE_GROUP = "SCREENING_TYPE_CD";
    public static final String SCREENING_RESULT_GROUP = "SCREENING_RESULT_CD";
    public static final String NOTE_TYPE_GROUP = "NOTE_TYPE_CD";
    /** admin에 이미 있는 그룹. 응급은 01·02·05만 쓴다 */
    public static final String CONSENT_TYPE_GROUP = "CONSENT_TYPE_CD";
    public static final String CONSENT_STATUS_GROUP = "CONSENT_STATUS_CD";
    public static final String CONSENT_BY_GROUP = "CONSENT_BY_CD";
    public static final String TREATMENT_TYPE_GROUP = "TREATMENT_TYPE_CD";
    public static final String CPR_EVENT_TYPE_GROUP = "CPR_EVENT_TYPE_CD";
    public static final String CPR_OUTCOME_GROUP = "CPR_OUTCOME_CD";
    public static final String ADMISSION_STATUS_GROUP = "ADMISSION_REQUEST_STATUS_CD";
    public static final String TRANSFER_HOSPITAL_GROUP = "TRANSFER_HOSPITAL_CD";
    /** admin에 이미 있는 그룹 */
    public static final String ADMIN_ROUTE_GROUP = "ADMIN_ROUTE_CD";
    /** admin에 이미 있는 그룹(진료과 01~18, 병동 01~08) — 입원요청에서 씀. admin이 꺼져 있으면 검증 생략 */
    public static final String DEPT_GROUP = "DEPT_CD";
    public static final String WARD_GROUP = "WARD_CD";

    // ---- 구역 ----
    public static final String ZONE_RESUS = "01";
    public static final String ZONE_CRITICAL = "02";
    public static final String ZONE_URGENT = "03";
    public static final String ZONE_FAST_TRACK = "04";
    public static final String ZONE_PEDIATRIC = "05";
    public static final String ZONE_ISOLATION = "06";
    public static final List<String> ZONE_FALLBACK =
            List.of(ZONE_RESUS, ZONE_CRITICAL, ZONE_URGENT, ZONE_FAST_TRACK, ZONE_PEDIATRIC, ZONE_ISOLATION);

    // ---- 병상 상태 (혼잡도 집계·배정/해제에서 값으로 분기) ----
    public static final String BED_STATUS_EMPTY = "01";
    public static final String BED_STATUS_OCCUPIED = "02";
    public static final String BED_STATUS_CLEANING = "03";
    public static final String BED_STATUS_OUT_OF_SERVICE = "04";
    public static final List<String> BED_STATUS_FALLBACK =
            List.of(BED_STATUS_EMPTY, BED_STATUS_OCCUPIED, BED_STATUS_CLEANING, BED_STATUS_OUT_OF_SERVICE);

    // ---- 병상 유형 (DB에 있던 값 기준: 일반/처치/격리실) ----
    public static final String BED_TYPE_GENERAL = "01";
    public static final String BED_TYPE_TREATMENT = "02";
    public static final String BED_TYPE_ISOLATION_ROOM = "03";
    public static final List<String> BED_TYPE_FALLBACK =
            List.of(BED_TYPE_GENERAL, BED_TYPE_TREATMENT, BED_TYPE_ISOLATION_ROOM);

    // ---- 퇴실 유형 ----
    public static final String DISPOSITION_HOME = "01";
    public static final String DISPOSITION_ADMIT = "02";
    public static final String DISPOSITION_TRANSFER = "03";
    public static final String DISPOSITION_DEATH = "04";
    public static final String DISPOSITION_DAMA = "05";
    public static final List<String> DISPOSITION_TYPE_FALLBACK =
            List.of(DISPOSITION_HOME, DISPOSITION_ADMIT, DISPOSITION_TRANSFER, DISPOSITION_DEATH, DISPOSITION_DAMA);

    // ---- KTAS 등급 (admin TRIAGE_CD: 01~05) ----
    public static final List<String> KTAS_LEVEL_FALLBACK = List.of("01", "02", "03", "04", "05");

    // ---- 중증도 평가 종류 (최초/재평가: 코드값으로 분기) ----
    public static final String ASSESSMENT_INITIAL = "01";
    public static final String ASSESSMENT_REASSESS = "02";
    public static final List<String> ASSESSMENT_TYPE_FALLBACK = List.of(ASSESSMENT_INITIAL, ASSESSMENT_REASSESS);

    // ---- 격리 유형 ----
    public static final List<String> ISOLATION_TYPE_FALLBACK = List.of("01", "02", "03", "04");

    // ---- 위험 스크리닝 ----
    public static final String SCREENING_SEPSIS = "01";
    public static final String SCREENING_STROKE = "02";
    public static final List<String> SCREENING_TYPE_FALLBACK = List.of(SCREENING_SEPSIS, SCREENING_STROKE);
    public static final String SCREENING_NEGATIVE = "01";
    public static final String SCREENING_POSITIVE = "02";
    public static final String SCREENING_INCONCLUSIVE = "03";
    public static final List<String> SCREENING_RESULT_FALLBACK =
            List.of(SCREENING_NEGATIVE, SCREENING_POSITIVE, SCREENING_INCONCLUSIVE);

    // ---- 진료기록 종류 ----
    public static final List<String> NOTE_TYPE_FALLBACK = List.of("01", "02", "03", "04");

    // ---- 동의 ----
    /** 응급이 쓰는 동의서 종류: 01 수술, 02 마취, 05 침습적 시술 (admin CONSENT_TYPE_CD의 일부) */
    public static final List<String> CONSENT_TYPE_ALLOWED = List.of("01", "02", "05");
    public static final String CONSENT_STATUS_AGREED = "01";
    public static final String CONSENT_STATUS_REFUSED = "02";
    public static final String CONSENT_STATUS_DEFERRED = "03";
    public static final List<String> CONSENT_STATUS_FALLBACK =
            List.of(CONSENT_STATUS_AGREED, CONSENT_STATUS_REFUSED, CONSENT_STATUS_DEFERRED);
    public static final String CONSENT_BY_SELF = "01";
    public static final String CONSENT_BY_GUARDIAN = "02";
    public static final List<String> CONSENT_BY_FALLBACK = List.of(CONSENT_BY_SELF, CONSENT_BY_GUARDIAN);

    // ---- 처치 / CPR / 투약 ----
    /** admin ADMIN_ROUTE_CD: 01 PO, 02 IV, 03 IM, 04 SC, 05 Topical, 06 Other */
    public static final List<String> ADMIN_ROUTE_FALLBACK = List.of("01", "02", "03", "04", "05", "06");
    public static final List<String> TREATMENT_TYPE_FALLBACK = List.of("01", "02", "03", "04", "05", "06", "07", "08");
    public static final List<String> CPR_EVENT_TYPE_FALLBACK = List.of("01", "02", "03", "04", "05", "06");
    public static final List<String> CPR_OUTCOME_FALLBACK = List.of("01", "02", "03");

    // ---- 입원요청 상태 (응급→병동 직접 이벤트) ----
    public static final String ADMISSION_REQUESTED = "01";
    public static final String ADMISSION_BED_ASSIGNED = "02";
    public static final String ADMISSION_REJECTED = "03";
    public static final List<String> ADMISSION_STATUS_FALLBACK =
            List.of(ADMISSION_REQUESTED, ADMISSION_BED_ASSIGNED, ADMISSION_REJECTED);

    // ---- 전원 대상 병원(샘플) ----
    public static final List<String> TRANSFER_HOSPITAL_FALLBACK = List.of("01", "02", "03", "04", "05", "06");
}
