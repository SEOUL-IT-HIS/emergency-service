package kr.co.seoulit.his.emergencyservice.monitor;

import kr.co.seoulit.his.emergencyservice.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 장기체류 환자를 주기적으로 찾아 LOS_ALERT 를 만든다 (UC-MON-02, Jira UD2-37). */
@ConditionalOnProperty(name = "app.monitor.los-alert-job.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
@Component
@RequiredArgsConstructor
public class LosAlertScheduler {

    private final MonitorService monitorService;

    @Scheduled(initialDelayString = "${app.monitor.los-alert-job.initial-delay-ms:60000}",
            fixedDelayString = "${app.monitor.los-alert-job.interval-ms:300000}")
    public void detectLongStayPatients() {
        try {
            int created = monitorService.detectLongStayPatients();
            if (created > 0) {
                log.info("[LOS] 장기체류 알림 {}건 생성", created);
            }
        } catch (Exception e) {
            log.error("[LOS] 장기체류 알림 생성 중 예외", e);
        }
    }
}
