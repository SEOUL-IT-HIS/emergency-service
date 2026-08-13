package kr.co.seoulit.his.emergencyservice.commoncode;

import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * admin-service 공통코드 로컬 캐시.
 * 서버 기동 시(CommonCodeCacheLoader) 한 번 채워지고, 그 이후 요청은 전부 이 메모리에서
 * 읽는다 — admin-service 를 매번 호출하지 않고, admin 이 잠깐 죽어도 이 캐시로 버틴다.
 * 단점: admin 쪽 코드가 바뀌어도 이 서버를 재시작하기 전까지는 반영되지 않는다.
 */
@Component
public class CommonCodeCache {

    private final Map<String, List<AdminCommonCodeDto>> cache = new ConcurrentHashMap<>();

    public void put(String groupCode, List<AdminCommonCodeDto> codes) {
        cache.put(groupCode, codes);
    }

    /** 캐싱된 적 없는 그룹이면 빈 리스트를 반환한다(예외 아님). */
    public List<AdminCommonCodeDto> get(String groupCode) {
        return cache.getOrDefault(groupCode, List.of());
    }

    public boolean isLoaded(String groupCode) {
        return cache.containsKey(groupCode);
    }
}
