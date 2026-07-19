# Emergency Service (응급관리 / UD2)

HIS(병원정보시스템) MSA 중 **응급(EMG)** 도메인을 담당하는 Spring Boot 서비스입니다.  
서비스 코드: `EMG` · 프론트 서비스 브랜치: `service/emergency` · 폴더명: `emergency`

## 문서 목록

| 문서 | 설명 |
| --- | --- |
| [requirements.md](./requirements.md) | 유스케이스·기능 요구사항·리스크 |
| [architecture.md](./architecture.md) | MSA 경계, SoT, 기술 스택, 설계 원칙 |
| [api.md](./api.md) | EMG Provider / Consumer API 명세 |
| [models.md](./models.md) | 도메인 모델·Oracle 테이블·업무코드 |
| [flow.md](./flow.md) | 주요 업무 흐름·시퀀스 다이어그램 |

## 프로젝트 개요

응급실 환자 여정(내원 → 중증도평가 → 자원배정 → 진료/투여 → 모니터링 → 퇴실·전원)을 지원합니다.

| 구분 | 내용 |
| --- | --- |
| Bounded Context | 응급 전용 업무 (Triage / Resource / Care / Monitor / Disposition / Codes / Channel) |
| 처방 Source of Truth | **GR2 처방코어** (`/api/orders*`) — EMG는 Consumer |
| 투여·처치(MAR) | **EMG 소유** (`orderId`는 GR2 참조) |
| DBMS | Oracle 단일 사용 |
| Frontend | Next.js 단일 Repository의 `emergency` 영역 (Redux + Redux-Saga) |

## 책임 범위 (한눈에)

**EMG가 소유(Provider)**

- KTAS·활력징후·격리·위험 스크리닝
- 구역/병상/의료기기 배정
- 진료기록·처치·MAR·CPR
- 현황판·장기체류 알림
- 퇴실·전원·이송 기록
- 응급 전용 업무코드
- 협진·당직 채널 요청(처방 원장 아님)

**EMG가 소비(Consumer)**

- 처방 CRUD / 구두처방 / DUR / 라우팅 → GR2
- 환자·알레르기 → PAT
- 검사결과 → LAB
- 조제상태 → PHM
- 공통코드(KTAS 등급명 등) → ADM
- 접수 연계 → RCP

## 기술 스택 (백엔드)

| 항목 | 내용 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.4.1 (hospital-backend-starter 정렬) |
| Build | Gradle |
| Persistence | Spring Data JPA + MapStruct (MyBatis 의존성 포함) |
| Package | `kr.co.seoulit.his.emergencyservice` |
| DB | Oracle (`EMG` 스키마), 로컬 테스트는 H2 |
| 응답 | `ApiResponse{code,message,data}` |
| 포트 | 8085 |

## 패키지 구조

```text
kr.co.seoulit.his.emergencyservice
├── common/          ApiResponse, CorsConfig, ExceptionHandler
├── triage/          상태평가 API
├── resource/        병상·기기
├── care/            진료·MAR·CPR
├── channel/         협진·당직
├── monitor/         현황판·LOS
├── disposition/     퇴실·전원
└── code/            응급 업무코드
```

각 도메인: `controller` → `service`/`ServiceImpl` → `repository` + `entity` + `dto` (+ MapStruct `mapper`)

## 로컬 실행

```bash
./gradlew bootRun
```

> 현재 저장소는 애플리케이션 스캐폴딩 단계입니다. API·엔티티 구현은 본 문서의 명세를 기준으로 진행합니다.

## 관련 산출물 (원본 명세)

- `개발표준가이드.md` — 브랜치·API·DB·MSA 원칙
- `HIS 응급관리.uml` — 유스케이스(Epic/Story)
- `UD2_응급관리_유스케이스_하위작업매핑.xlsx`
- `응급관리_API_목록(응급전용제외).xlsx`
- `전체서비스_API카탈로그.xlsx`
- `ER_HIS_dbdiagram_oracle_1.dbml` / `ER_EMG_domain_code_oracle.dbml`

## 문서 기준일

- API·아키텍처 확정 반영: **2026-07-16**
- 처방코어(GR2) 통합·스냅샷 폐지: **2026-07-15 / 2026-07-10**
