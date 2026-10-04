-- ============================================================
-- 입원요청에 "병동이 배정한 병동" 컬럼 추가 (병동팀 합의 2026-09-30)
-- 화면의 병동은 희망 병동(wardPref)이 아니라 병동의 BED_ASSIGNED 회신값(wardCode)으로 보여준다.
-- 희망 병동에 빈 병상이 없으면 다른 병동으로 배정될 수 있기 때문.
-- 값은 admin 공통코드 WARD_CD 의 코드값. 새 코드를 띄우기 전에 먼저 실행할 것
-- (컬럼이 없는 상태에서 새 코드가 뜨면 입원요청 조회/저장이 오류난다).
-- ============================================================
ALTER TABLE EMERGENCY.ADMISSION_REQUEST ADD (ASSIGNED_WARD_CODE VARCHAR2(20));

COMMENT ON COLUMN EMERGENCY.ADMISSION_REQUEST.ASSIGNED_WARD_CODE IS '병동이 배정한 병동(WARD_CD, BED_ASSIGNED 회신의 wardCode)';
