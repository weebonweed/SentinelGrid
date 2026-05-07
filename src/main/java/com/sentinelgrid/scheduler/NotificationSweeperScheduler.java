package com.sentinelgrid.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.sentinelgrid.service.interfaces.NotificationService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NotificationSweeperScheduler {

    private final NotificationService notificationService;

    public NotificationSweeperScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${sentinelgrid.scheduler.notification-sweeper-cron}")
    public void sweepPendingNotifications() {
        log.info("Notification sweeper started");
        try {
            notificationService.processAllPending();
            log.info("Notification sweeper completed");
        } catch (Exception e) {
            log.error("Notification sweeper failed with error: {}", e.getMessage(), e);
        }
    }
}
