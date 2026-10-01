package kr.co.seoulit.his.emergencyservice.resource.service;

import kr.co.seoulit.his.emergencyservice.resource.dto.*;

import java.util.List;

public interface ResourceService {
    /** 퇴실 완료로 병상을 자동 해제할 때 해제자(releasedById)로 남기는 값 */
    String SYSTEM_ACTOR = "SYSTEM";

    CongestionDto getCongestion();
    List<BedDto> getBeds(String zoneCode, String status);
    BedAssignmentDto assignBed(BedAssignmentCreateRequestDto request);
    BedAssignmentDto releaseBed(String assignmentId, BedReleaseRequestDto request);

    /** 접수의 현재(해제 안 된) 병상 배정. 없으면 null. 화면이 새로고침·환자 전환 뒤에도 Release 를 보여주려고 쓴다. */
    BedAssignmentDto getCurrentAssignment(String receptionId);

    /** 접수의 해제 안 된 병상 배정을 모두 해제하고 병상을 EMPTY 로 되돌린다. 해제한 건수 반환(없으면 0, 여러 번 불러도 안전). */
    int releaseBedsOf(String receptionId, String releasedById);
}
