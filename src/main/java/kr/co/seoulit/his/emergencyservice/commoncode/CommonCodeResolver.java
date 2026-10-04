package kr.co.seoulit.his.emergencyservice.commoncode;

import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공통코드 값 조회 도우미 — admin 캐시에 그룹이 있으면 사용 중(useYn != N)인 값들을, 없으면 폴백을 돌려준다.
 * 서비스마다 같은 조회 코드를 되풀이하지 않으려고 모았다.
 */
@Component
@RequiredArgsConstructor
public class CommonCodeResolver {

    private final CommonCodeCache commonCodeCache;

    /** 순서 유지(admin 정렬 순서 그대로) */
    public List<String> values(String groupCode, List<String> fallback) {
        List<AdminCommonCodeItemDto> codes = commonCodeCache.get(groupCode);
        if (codes.isEmpty()) {
            return fallback;
        }
        return codes.stream()
                .filter(code -> !"N".equals(code.getUseYn()))
                .map(AdminCommonCodeItemDto::getCodeValue)
                .collect(Collectors.toList());
    }

    public Set<String> valueSet(String groupCode, List<String> fallback) {
        return new LinkedHashSet<>(values(groupCode, fallback));
    }

    /** 값이 허용 목록에 없으면 IllegalArgumentException(→ 400). 메시지의 허용값은 정렬해서 보여준다. */
    public void require(String field, String value, Set<String> allowed) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                    field + " must be one of " + allowed.stream().sorted().collect(Collectors.joining(", ")));
        }
    }
}
