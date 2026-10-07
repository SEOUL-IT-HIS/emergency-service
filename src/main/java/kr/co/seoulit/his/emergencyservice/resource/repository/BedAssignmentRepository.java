package kr.co.seoulit.his.emergencyservice.resource.repository;

import kr.co.seoulit.his.emergencyservice.resource.entity.BedAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BedAssignmentRepository extends JpaRepository<BedAssignment, String> {
    /** 지금 병상을 쓰고 있는(해제하지 않은) 배정이 있는지 — 접수 취소 판단에 쓴다 */
    boolean existsByReceptionIdAndReleasedAtIsNull(String receptionId);


    // 여러 접수의 현재(해제 안 된) 병상 배정을 병상 정보까지 한 번에 가져온다 — 환자 목록 N+1 방지.
    // JOIN FETCH 가 없으면 bed 가 LAZY 라서 배정마다 병상 조회가 한 번씩 더 나간다.
    @Query("SELECT ba FROM BedAssignment ba JOIN FETCH ba.bed "
            + "WHERE ba.receptionId IN :receptionIds AND ba.releasedAt IS NULL")
    List<BedAssignment> findActiveWithBedByReceptionIdIn(@Param("receptionIds") Collection<String> receptionIds);

    // 한 접수의 현재 배정(병상 포함) — 퇴실 완료 시 자동 해제, 화면의 현재 배정 조회에 쓴다.
    @Query("SELECT ba FROM BedAssignment ba JOIN FETCH ba.bed "
            + "WHERE ba.receptionId = :receptionId AND ba.releasedAt IS NULL ORDER BY ba.assignedAt DESC")
    List<BedAssignment> findActiveWithBedByReceptionId(@Param("receptionId") String receptionId);
}
