-- ============================================================
-- 환자 없이 점유로 남은 병상 정리 (Critical C-01, Urgent U-01/U-02 등)
-- 화면의 Release 는 "선택한 환자의 배정"만 풀 수 있다. 접수가 지워졌거나(RECEPTION_INTAKE 없음)
-- 병상 상태만 점유(02)로 남은 경우엔 선택할 환자가 없어 화면에서 풀 수 없다.
-- 1) 먼저 SELECT 로 확인하고, 2)~3) 은 결과를 보고 필요할 때만 실행한다. (DML — 실행 후 COMMIT)
-- ============================================================

-- 1) 점유(02) 병상과 그 병상을 쥔 배정·접수 (배정이 없거나 접수가 없으면 정리 대상)
SELECT b.BED_NO, b.BED_STATUS_CODE,
       ba.BED_ASSIGNMENT_ID, ba.RECEPTION_ID, ba.ASSIGNED_AT,
       CASE WHEN ba.BED_ASSIGNMENT_ID IS NULL THEN '배정 없음(상태만 점유)'
            WHEN ri.RECEPTION_ID IS NULL THEN '접수 없음(고아 배정)'
            ELSE '접수 있음' END AS CAUSE
  FROM EMERGENCY.BED b
  LEFT JOIN EMERGENCY.BED_ASSIGNMENT ba ON ba.BED_ID = b.BED_ID AND ba.RELEASED_AT IS NULL
  LEFT JOIN EMERGENCY.RECEPTION_INTAKE ri ON ri.RECEPTION_ID = ba.RECEPTION_ID
 WHERE b.BED_STATUS_CODE = '02'
 ORDER BY b.BED_NO;

-- 2) 접수가 없는 고아 배정을 해제 처리 (해제자 SYSTEM)
UPDATE EMERGENCY.BED_ASSIGNMENT ba
   SET ba.RELEASED_AT = SYSDATE, ba.RELEASED_BY_ID = 'SYSTEM', ba.UPDATED_AT = SYSDATE
 WHERE ba.RELEASED_AT IS NULL
   AND NOT EXISTS (SELECT 1 FROM EMERGENCY.RECEPTION_INTAKE ri WHERE ri.RECEPTION_ID = ba.RECEPTION_ID);

-- 3) 해제 안 된 배정이 없는데 점유(02)로 남은 병상을 비어 있음(01)으로
UPDATE EMERGENCY.BED b
   SET b.BED_STATUS_CODE = '01', b.UPDATED_AT = SYSDATE
 WHERE b.BED_STATUS_CODE = '02'
   AND NOT EXISTS (SELECT 1 FROM EMERGENCY.BED_ASSIGNMENT ba
                    WHERE ba.BED_ID = b.BED_ID AND ba.RELEASED_AT IS NULL);

-- COMMIT;

-- 확인: 점유 병상 수가 실제 해제 안 된 배정 수와 같아야 한다
-- SELECT (SELECT COUNT(*) FROM EMERGENCY.BED WHERE BED_STATUS_CODE = '02') AS OCCUPIED_BEDS,
--        (SELECT COUNT(*) FROM EMERGENCY.BED_ASSIGNMENT WHERE RELEASED_AT IS NULL) AS ACTIVE_ASSIGNMENTS
--   FROM DUAL;
