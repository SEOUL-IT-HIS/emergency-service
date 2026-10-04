-- ============================================================
-- EMERGENCY 스키마 - 동의 기록(CONSENT_RECORD) 테이블 생성 (Jira UD2-25)
-- 근거: UML 01-5 "동의 기록" — 종이 동의서를 받은 사실만 기록(서명·원본 파일 저장 없음)
-- 이력 테이블: 수정·삭제 없이 행을 추가한다(유예 후 사후 동의를 받으면 AGREED 행을 새로 추가).
-- 제약조건 이름 규칙은 기존 테이블과 동일: PK_{테이블}
-- RECEPTION_ID는 기존 테이블과 동일하게 FK 없음(접수는 RCP 소유 데이터라 참조 식별자만 보유, CLAUDE.md §9)
-- *_CODE 컬럼은 CHECK 제약을 걸지 않음 — 코드값은 admin 공통코드가 소유(개발표준 21.4), 검증은 애플리케이션에서 수행
--   CONSENT_TYPE_CODE   : 01 / 02 / 05 (admin 그룹 CONSENT_TYPE_CD 중 응급이 쓰는 값)
--   CONSENT_STATUS_CODE : 01 동의 / 02 거부 / 03 유예 (admin 그룹 ER_CONSENT_STATUS_CD)
--   CONSENTED_BY_CODE   : 01 본인 / 02 보호자       (admin 그룹 CONSENT_BY_CD)
-- 실행: sqlplus 로 EMERGENCY 계정 접속 후  @create-consent-record.sql
-- ============================================================

CREATE TABLE EMERGENCY.CONSENT_RECORD (
    CONSENT_RECORD_ID   VARCHAR2(36)  NOT NULL ENABLE,
    RECEPTION_ID        VARCHAR2(36)  NOT NULL ENABLE,
    CONSENT_TYPE_CODE   VARCHAR2(20)  NOT NULL ENABLE,
    CONSENT_STATUS_CODE VARCHAR2(20)  NOT NULL ENABLE,
    CONSENTED_BY_CODE   VARCHAR2(20)  NOT NULL ENABLE,
    CONSENTER_NAME      VARCHAR2(100),
    REASON              VARCHAR2(500),
    RECEIVED_AT         TIMESTAMP(6)  NOT NULL ENABLE,
    RECORDED_BY_ID      VARCHAR2(36)  NOT NULL ENABLE,
    RECORDED_AT         TIMESTAMP(6)  NOT NULL ENABLE,
    CREATED_AT          TIMESTAMP(6),
    UPDATED_AT          TIMESTAMP(6),
    CONSTRAINT PK_CONSENT_RECORD PRIMARY KEY (CONSENT_RECORD_ID)
);

-- 접수 건별 목록 조회(최신 수령 일시순)용
CREATE INDEX EMERGENCY.IX_CONSENT_RECORD_RECEPTION ON EMERGENCY.CONSENT_RECORD (RECEPTION_ID, RECEIVED_AT);

COMMENT ON TABLE  EMERGENCY.CONSENT_RECORD                     IS '동의 기록(종이 동의서 수령 사실)';
COMMENT ON COLUMN EMERGENCY.CONSENT_RECORD.CONSENTER_NAME      IS '동의자 이름(보호자 동의일 때 필수)';
COMMENT ON COLUMN EMERGENCY.CONSENT_RECORD.REASON              IS '유예/거부 사유(유예일 때 필수)';
COMMENT ON COLUMN EMERGENCY.CONSENT_RECORD.RECEIVED_AT         IS '동의서 수령 일시';
COMMENT ON COLUMN EMERGENCY.CONSENT_RECORD.RECORDED_BY_ID      IS '기록자 ID';
