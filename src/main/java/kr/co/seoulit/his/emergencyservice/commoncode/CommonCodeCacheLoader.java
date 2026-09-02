package kr.co.seoulit.his.emergencyservice.commoncode;

import kr.co.seoulit.his.emergencyservice.commoncode.client.AdminCommonCodeClient;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeGroupDto;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서버 기동 시 admin-service 공통코드를 "전부" 미리 받아 CommonCodeCache 에 적재한다.
 * (팀 합의: 그룹을 골라서 캐싱하는 대신 한 번에 전부 받아오는 단순한 방식으로 진행)
 *
 * 1) GET /api/commonCodeGroup/list 로 전체 그룹 목록을 받고
 * 2) 그룹마다 GET /api/commonCodeItem/list?groupId= 로 항목을 받아
 * 3) groupCode 를 key 로 CommonCodeCache 에 적재한다.
 *
 * admin-service 가 그 순간 꺼져있어도 emergency-service 기동 자체는 막지 않는다 —
 * 그룹 목록 조회 자체가 실패하면 전체를 건너뛰고, 특정 그룹의 항목 조회만 실패하면
 * 그 그룹만 빈 채로 남기고 나머지는 계속 진행한다.
 */
@Component
@RequiredArgsConstructor
public class CommonCodeCacheLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CommonCodeCacheLoader.class);

    private final AdminCommonCodeClient adminCommonCodeClient;
    private final CommonCodeCache commonCodeCache;

    @Override
    public void run(String... args) {
        List<AdminCommonCodeGroupDto> groups;
        try {
            groups = adminCommonCodeClient.getGroups();
        } catch (Exception e) {
            log.warn("공통코드 그룹 목록 조회 실패 (admin-service 응답 없음 등) - 캐시 비어있는 채로 기동합니다: {}", e.getMessage());
            return;
        }

        for (AdminCommonCodeGroupDto group : groups) {
            try {
                List<AdminCommonCodeItemDto> items = adminCommonCodeClient.getItems(group.getGroupId());
                commonCodeCache.put(group.getGroupCode(), items);
                log.info("공통코드 캐시 적재 완료: {} ({}건)", group.getGroupCode(), items.size());
            } catch (Exception e) {
                log.warn("공통코드 캐시 적재 실패: {} - {}", group.getGroupCode(), e.getMessage());
            }
        }
    }
}
