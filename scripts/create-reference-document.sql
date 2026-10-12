-- ============================================================
-- EMERGENCY 스키마 - 응급 참고 문서(REFERENCE_DOCUMENT) 테이블 생성
-- 응급 직원이 찾아 보는 프로토콜·지침·서식 파일의 목록. 파일 본문은 DB 가 아니라 SeaweedFS(S3) 버킷에 두고,
-- 여기에는 제목·분류·올린 사람과 버킷 안의 객체 키(OBJECT_KEY)만 둔다. 환자·접수와 무관한 자료라 접수 ID 가 없다.
-- 삭제는 DELETED_YN='Y' 상태 변경이다(행과 파일은 남긴다).
-- 제약조건 이름 규칙은 기존 테이블과 동일: PK_{테이블}
-- UPLOADED_BY_ID / DELETED_BY_ID 는 admin 직원 ID(empId) — FK 없음(타 서비스 소유 데이터, CLAUDE.md §9)
-- 새 코드를 띄우기 전에 먼저 실행할 것 — 테이블이 없으면 참고 문서 화면만 오류가 난다(다른 응급 기능에는 영향 없음).
-- 실행: sqlplus 로 EMERGENCY 계정 접속 후  @create-reference-document.sql
-- ============================================================

CREATE TABLE EMERGENCY.REFERENCE_DOCUMENT (
    REFERENCE_DOCUMENT_ID VARCHAR2(36)  NOT NULL ENABLE,
    TITLE                 VARCHAR2(200) NOT NULL ENABLE,
    CATEGORY              VARCHAR2(50),
    FILE_NAME             VARCHAR2(255) NOT NULL ENABLE,
    CONTENT_TYPE          VARCHAR2(100) NOT NULL ENABLE,
    FILE_SIZE             NUMBER(12)    NOT NULL ENABLE,
    OBJECT_KEY            VARCHAR2(300) NOT NULL ENABLE,
    UPLOADED_BY_ID        VARCHAR2(36)  NOT NULL ENABLE,
    UPLOADED_AT           TIMESTAMP(6)  NOT NULL ENABLE,
    DELETED_YN            VARCHAR2(1)   DEFAULT 'N' NOT NULL ENABLE,
    DELETED_BY_ID         VARCHAR2(36),
    DELETED_AT            TIMESTAMP(6),
    CREATED_AT            TIMESTAMP(6),
    UPDATED_AT            TIMESTAMP(6),
    CONSTRAINT PK_REFERENCE_DOCUMENT PRIMARY KEY (REFERENCE_DOCUMENT_ID)
);

-- 삭제되지 않은 문서를 최신순으로 보여 주는 목록 조회용
CREATE INDEX EMERGENCY.IX_REFERENCE_DOCUMENT_LIST ON EMERGENCY.REFERENCE_DOCUMENT (DELETED_YN, UPLOADED_AT);

COMMENT ON TABLE  EMERGENCY.REFERENCE_DOCUMENT                       IS '응급 참고 문서(프로토콜·지침·서식 파일 목록)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.TITLE                 IS '문서 제목(화면 표시용, 200자)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.CATEGORY              IS '분류(자유 입력, 50자)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.FILE_NAME             IS '올릴 때의 원래 파일 이름(내려받을 때 이 이름으로 줌)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.CONTENT_TYPE          IS '파일 형식(확장자에서 서버가 정함)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.FILE_SIZE             IS '파일 크기(바이트)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.OBJECT_KEY            IS 'SeaweedFS 버킷 안의 객체 키(서버가 만든 이름)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.UPLOADED_BY_ID        IS '올린 직원 ID(admin empId)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.UPLOADED_AT           IS '올린 일시';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.DELETED_YN            IS '삭제 여부(Y면 목록·내려받기에서 빠짐, 행과 파일은 남김)';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.DELETED_BY_ID         IS '삭제한 직원 ID';
COMMENT ON COLUMN EMERGENCY.REFERENCE_DOCUMENT.DELETED_AT            IS '삭제 일시';

-- 확인: 컬럼 14개가 보여야 한다
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'EMERGENCY' AND table_name = 'REFERENCE_DOCUMENT'
 ORDER BY column_id;
