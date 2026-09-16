package kr.co.seoulit.his.common.session;

import lombok.Data;

/**
 * 세션에 담는 로그인 사용자.
 *
 * 모든 MSA 서비스가 이 파일을 그대로 복사해서 쓴다.
 * 패키지 경로까지 똑같아야 한다 (Redis JSON 의 "@class" 에 적히기 때문).
 * 필드 이름이 팀 간 약속이다. 바꾸면 다른 서비스가 깨진다.
 *
 * List 를 쓰지 않는 이유 - "@class" 표시가 List 에도 붙어서
 * ["java.util.ArrayList",["01"]] 처럼 감싸진다. 그래서 쉼표 문자열로 담는다.
 */
@Data
public class SessionUser {

    /** ACCOUNT.ACCOUNT_ID */
    private String accountId;

    /** 계정 상태 (01 = 정상) */
    private String accountStatus;

    /** EMPLOYEE.EMP_ID - "작성자" 같은 값으로 저장할 때 쓰는 키 */
    private String empId;

    /** 사번 (예: E202608001) */
    private String empNo;

    /** 직원 이름 */
    private String empName;

    /** 부서 공통코드 (DEPT_CD) */
    private String deptCode;

    /** 로그인 아이디 */
    private String loginId;

    /** 역할 코드들. 쉼표로 이어 붙인다 (예: "01" 또는 "01,02"). 없으면 "" */
    private String roleCodes;

    /** 볼 수 있는 메뉴 코드들. 쉼표로 이어 붙인다. 없으면 "" */
    private String menuCodes;
}
