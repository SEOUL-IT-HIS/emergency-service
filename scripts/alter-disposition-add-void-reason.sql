-- ============================================================
-- 퇴실 결정에 "오접수·중복 접수" 사유 컬럼 3개 추가 (접수 RCP 합의 2026-10-08: ReceptionVoided 이벤트)
-- 응급이 퇴실 유형 06(오접수·중복 접수)으로 접수를 닫으면 사유를 남기고 접수 서비스에 알려, 접수가 그 접수를 취소 처리한다.
--   VOID_REASON_TYPE_CODE : DUPLICATE(중복 접수) / ERRONEOUS(접수 오류) — 06 일 때만 값이 있다
--   VOID_REASON           : 사유(자유 입력, 500자)
--   KEPT_RECEPTION_ID     : 중복일 때 남기는 접수 ID(선택, 응급의 접수 ID)
-- 세 컬럼 모두 NULL 허용이라 기존 행에는 영향이 없다(06 이 아닌 결정은 NULL).
-- 새 코드를 띄우기 전에 먼저 실행할 것 — 컬럼이 없는 상태에서 새 코드가 뜨면 퇴실 결정 조회/저장이 모두 오류난다(ORA-00904).
-- 퇴실 유형 06 은 admin 공통코드(ER_DISPOSITION_TYPE_CD)에 추가해야 화면에 보이고 서버가 받는다(admin 요청 필요).
-- ============================================================
ALTER TABLE EMERGENCY.DISPOSITION ADD (
    VOID_REASON_TYPE_CODE VARCHAR2(20),
    VOID_REASON           VARCHAR2(500),
    KEPT_RECEPTION_ID     VARCHAR2(36)
);

COMMENT ON COLUMN EMERGENCY.DISPOSITION.VOID_REASON_TYPE_CODE IS '오접수·중복 접수(퇴실 유형 06)의 사유 유형: DUPLICATE 중복 접수 / ERRONEOUS 접수 오류';
COMMENT ON COLUMN EMERGENCY.DISPOSITION.VOID_REASON           IS '오접수·중복 접수의 사유(자유 입력, 500자 이내)';
COMMENT ON COLUMN EMERGENCY.DISPOSITION.KEPT_RECEPTION_ID     IS '중복 접수일 때 남기는 접수 ID(선택)';

-- 확인: 컬럼 3개가 보여야 한다
SELECT column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'EMERGENCY' AND table_name = 'DISPOSITION'
   AND column_name IN ('VOID_REASON_TYPE_CODE', 'VOID_REASON', 'KEPT_RECEPTION_ID')
 ORDER BY column_name;
