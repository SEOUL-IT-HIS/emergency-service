package kr.co.seoulit.his.emergencyservice.commoncode.controller;

import kr.co.seoulit.his.emergencyservice.common.config.EmergencyExceptionHandler;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCache;
import kr.co.seoulit.his.emergencyservice.commoncode.CommonCodeCacheLoader;
import kr.co.seoulit.his.emergencyservice.commoncode.client.AdminCommonCodeClient;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeGroupDto;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 기동 직후 적재가 끝나기 전에는 비었거나 일부만 있는 공통코드를 내주지 않고 "준비 중"으로 답한다 */
class CommonCodeControllerTest {

    private CommonCodeCache cache;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        cache = new CommonCodeCache();
        mvc = MockMvcBuilders.standaloneSetup(new CommonCodeController(cache))
                .setControllerAdvice(new EmergencyExceptionHandler())
                .build();
    }

    private static AdminCommonCodeItemDto item(String value, String name) {
        AdminCommonCodeItemDto dto = new AdminCommonCodeItemDto();
        dto.setCodeValue(value);
        dto.setCodeName(name);
        dto.setUseYn("Y");
        return dto;
    }

    @Test
    void 적재가_끝나기_전에는_준비_중으로_답한다() throws Exception {
        cache.put("WARD_CD", List.of(item("01", "Internal Medicine Ward")));   // 일부만 적재된 상태

        mvc.perform(get("/api/emergency/codes/common"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("EMG_UPSTREAM_ERROR"));
        mvc.perform(get("/api/emergency/codes/common/WARD_CD")).andExpect(status().isBadGateway());
    }

    @Test
    void 적재가_끝나면_전체와_그룹별_조회를_준다() throws Exception {
        cache.put("WARD_CD", List.of(item("01", "Internal Medicine Ward")));
        cache.markReady();

        mvc.perform(get("/api/emergency/codes/common"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.WARD_CD[0].codeName").value("Internal Medicine Ward"));
        mvc.perform(get("/api/emergency/codes/common/WARD_CD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].codeValue").value("01"));
    }

    @Test
    void 적재기는_admin이_꺼져_있어도_끝나면_준비됨으로_표시한다() {
        AdminCommonCodeClient client = mock(AdminCommonCodeClient.class);
        when(client.getGroups()).thenThrow(new IllegalStateException("admin down"));
        CommonCodeCache emptyCache = new CommonCodeCache();

        new CommonCodeCacheLoader(client, emptyCache).run();

        assertThat(emptyCache.isReady()).isTrue();
        assertThat(emptyCache.getAll()).isEmpty();
    }

    @Test
    void 적재기는_그룹을_적재한_뒤_준비됨으로_표시한다() {
        AdminCommonCodeClient client = mock(AdminCommonCodeClient.class);
        AdminCommonCodeGroupDto group = new AdminCommonCodeGroupDto();
        group.setGroupId("g1");
        group.setGroupCode("WARD_CD");
        when(client.getGroups()).thenReturn(List.of(group));
        when(client.getItems("g1")).thenReturn(List.of(item("01", "Internal Medicine Ward")));
        CommonCodeCache loaded = new CommonCodeCache();
        assertThat(loaded.isReady()).isFalse();

        new CommonCodeCacheLoader(client, loaded).run();

        assertThat(loaded.isReady()).isTrue();
        assertThat(loaded.get("WARD_CD")).hasSize(1);
    }
}
