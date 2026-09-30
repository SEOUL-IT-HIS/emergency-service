package kr.co.seoulit.his.emergencyservice.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Oracle IN 목록 1000개 제한 — 나눠서 조회하고 결과를 합치는지 */
class InChunksTest {

    @Test
    void splitsIntoChunksOfAtMostOneThousand() {
        List<String> ids = IntStream.range(0, 2500).mapToObj(i -> "id-" + i).toList();
        List<Integer> chunkSizes = new ArrayList<>();

        List<String> result = InChunks.query(ids, chunk -> {
            chunkSizes.add(chunk.size());
            return chunk;
        });

        assertThat(chunkSizes).containsExactly(1000, 1000, 500);
        assertThat(result).hasSize(2500);
    }

    @Test
    void emptyIdsDoNotQueryAndDuplicatesAreRemoved() {
        List<Integer> calls = new ArrayList<>();
        assertThat(InChunks.query(List.of(), chunk -> { calls.add(1); return chunk; })).isEmpty();
        assertThat(calls).isEmpty();

        assertThat(InChunks.query(List.of("a", "a", "b"), chunk -> chunk)).containsExactly("a", "b");
    }
}
