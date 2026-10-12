package kr.co.seoulit.his.emergencyservice.commoncode;

import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * admin-service 공통코드 로컬 캐시.
 * 서버 기동 시(CommonCodeCacheLoader) admin 이 가진 그룹 전체를 한 번에 채우고,
 * 그 이후 요청은 전부 이 메모리에서 읽는다 — admin-service 를 매번 호출하지 않고,
 * admin 이 잠깐 죽어도 이 캐시로 버틴다.
 * 단점: admin 쪽 코드가 바뀌어도 이 서버를 재시작하기 전까지는 반영되지 않는다.
 */
@Component
public class CommonCodeCache {

    private final Map<String, List<AdminCommonCodeItemDto>> cache = new ConcurrentHashMap<>();

    /**
     * 기동 시 적재(CommonCodeCacheLoader)가 끝났는지. 적재는 웹 서버가 요청을 받기 시작한 뒤에 돌기 때문에, 그 사이에 화면이
     * 공통코드를 조회하면 비었거나 일부만 오는데 화면은 그 결과를 그대로 쓴다(우선순위가 STAT 하나만 보이고 진료과·병동 선택칸이
     * 입력칸으로 바뀌는 증상). 적재가 끝나기 전에는 조회 API 가 "아직 준비 중"으로 답해 화면이 다시 시도하게 한다.
     * admin 이 꺼져 있어 비어 있는 채로 끝나도 준비됨으로 본다(계속 막지 않는다).
     */
    private volatile boolean ready;

    public boolean isReady() {
        return ready;
    }

    public void markReady() {
        this.ready = true;
    }

    public void put(String groupCode, List<AdminCommonCodeItemDto> codes) {
        cache.put(groupCode, codes);
    }

    /** 캐싱된 적 없는 그룹이면 빈 리스트를 반환한다(예외 아님). */
    public List<AdminCommonCodeItemDto> get(String groupCode) {
        return cache.getOrDefault(groupCode, List.of());
    }

    public boolean isLoaded(String groupCode) {
        return cache.containsKey(groupCode);
    }

    /** 전체 캐시(그룹코드 -> 항목 목록) 를 그대로 반환한다. */
    public Map<String, List<AdminCommonCodeItemDto>> getAll() {
        return cache;
    }
}
