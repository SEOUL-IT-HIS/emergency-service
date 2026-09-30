-- ============================================================
-- UD2-84 혼잡도 집계용 복합 인덱스 (2026-09-30 적용 완료)
-- 3번째 컬럼 BED_ID: Oracle B-tree는 인덱스 컬럼이 전부 null인 행을 저장하지 않는다.
--   ZONE_CODE/BED_STATUS_CODE 가 nullable 이라 2컬럼 인덱스로는 GROUP BY 전체 집계를 보장할 수 없어
--   NOT NULL 인 PK를 붙였다. 결과: TABLE ACCESS FULL -> INDEX FULL SCAN + SORT GROUP BY NOSORT
-- ============================================================
CREATE INDEX EMERGENCY.IX_BED_ZONE_STATUS ON EMERGENCY.BED (ZONE_CODE, BED_STATUS_CODE, BED_ID);

-- 실행계획 확인
-- EXPLAIN PLAN FOR
--   SELECT zone_code, bed_status_code, COUNT(*) FROM emergency.bed GROUP BY zone_code, bed_status_code;
-- SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY(NULL, NULL, 'BASIC'));
