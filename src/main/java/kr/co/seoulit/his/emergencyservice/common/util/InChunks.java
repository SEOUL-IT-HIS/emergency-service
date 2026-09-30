package kr.co.seoulit.his.emergencyservice.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * ID 목록으로 한 번에 조회(WHERE ... IN (...))할 때 쓰는 도우미 — N+1(건마다 조회) 대신 사용한다.
 * Oracle 은 IN 목록이 1000개를 넘으면 ORA-01795 로 실패하므로 1000개씩 나눠 조회해서 합친다.
 * ID 가 비어 있으면 조회하지 않고 빈 목록을 돌려준다.
 */
public final class InChunks {

    public static final int ORACLE_IN_LIMIT = 1000;

    private InChunks() {
    }

    public static <T> List<T> query(Collection<String> ids, Function<List<String>, List<T>> finder) {
        List<String> distinct = ids.stream().distinct().toList();
        List<T> result = new ArrayList<>();
        for (int from = 0; from < distinct.size(); from += ORACLE_IN_LIMIT) {
            result.addAll(finder.apply(distinct.subList(from, Math.min(from + ORACLE_IN_LIMIT, distinct.size()))));
        }
        return result;
    }
}
