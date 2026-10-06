-- ============================================================
-- 응급 접수에 "취소 시각" 컬럼 추가 (접수 RCP 합의: 접수 취소 이벤트 ReceptionCancelled)
-- 접수를 취소하면 행을 지우지 않고 CANCELLED_AT 에 취소 시각을 남긴다. 값이 있으면 취소된 접수 → 환자 목록·현황판에서 빠진다.
-- 진료 기록(처치·투약·KTAS 직접 입력·병상·퇴실 결정·처방 등)이 있는 접수는 취소 이벤트를 받아도 취소하지 않는다.
-- 새 코드를 띄우기 전에 먼저 실행할 것
-- (컬럼이 없는 상태에서 새 코드가 뜨면 접수 조회/저장이 모두 오류난다).
-- ============================================================
ALTER TABLE EMERGENCY.RECEPTION_INTAKE ADD (CANCELLED_AT TIMESTAMP(6));

COMMENT ON COLUMN EMERGENCY.RECEPTION_INTAKE.CANCELLED_AT IS '접수 취소 시각(RCP ReceptionCancelled 이벤트의 occurredAt). NULL 이면 취소되지 않은 접수';
