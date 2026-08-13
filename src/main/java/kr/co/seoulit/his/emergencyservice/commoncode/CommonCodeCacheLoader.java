package kr.co.seoulit.his.emergencyservice.commoncode;

import kr.co.seoulit.his.emergencyservice.commoncode.client.AdminCommonCodeClient;
import kr.co.seoulit.his.emergencyservice.commoncode.dto.AdminCommonCodeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 서버 기동 시 admin-service 공통코드를 미리 받아 CommonCodeCache 에 적재한다.
 *
 * admin-service 가 그 순간 꺼져있어도 emergency-service 기동 자체는 막지 않는다 —
 * 그룹별로 개별 try/catch 하여 실패한 그룹만 빈 캐시로 남기고 넘어간다.
 * (필요하면 나중에 관리자가 수동 재적재 API 를 추가하거나, 서버를 재시작해 다시 채운다)
 *
 * cacheGroups 는 CorsConfig 와 동일하게 @ConfigurationProperties 로 바인딩한다
 * (YAML list -> List&lt;String&gt; 는 @Value 보다 이 방식이 안전하다).
 */
@Component
@ConfigurationProperties(prefix = "app.commoncode")
public class CommonCodeCacheLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CommonCodeCacheLoader.class);

    private final AdminCommonCodeClient adminCommonCodeClient;
    private final CommonCodeCache commonCodeCache;

    private List<String> cacheGroups = new ArrayList<>();

    public CommonCodeCacheLoader(AdminCommonCodeClient adminCommonCodeClient, CommonCodeCache commonCodeCache) {
        this.adminCommonCodeClient = adminCommonCodeClient;
        this.commonCodeCache = commonCodeCache;
    }

    public List<String> getCacheGroups() {
        return cacheGroups;
    }

    public void setCacheGroups(List<String> cacheGroups) {
        this.cacheGroups = cacheGroups;
    }

    @Override
    public void run(String... args) {
        for (String groupCode : cacheGroups) {
            try {
                List<AdminCommonCodeDto> codes = adminCommonCodeClient.getCommonCodes(groupCode);
                commonCodeCache.put(groupCode, codes);
                log.info("공통코드 캐시 적재 완료: {} ({}건)", groupCode, codes.size());
            } catch (Exception e) {
                log.warn("공통코드 캐시 적재 실패 (admin-service 응답 없음 등): {} - {}", groupCode, e.getMessage());
            }
        }
    }
}
