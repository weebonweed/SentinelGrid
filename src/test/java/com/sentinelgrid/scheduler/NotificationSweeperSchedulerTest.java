package com.sentinelgrid.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sentinelgrid.service.interfaces.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationSweeperSchedulerTest {

    @Mock private NotificationService notificationService;

    @InjectMocks
    private NotificationSweeperScheduler scheduler;

    @Test
    void sweep_delegatesToNotificationService() {
        scheduler.sweepPendingNotifications();
        verify(notificationService, times(1)).processAllPending();
    }

    @Test
    void sweep_isIdempotent_whenCalledMultipleTimes() {
        scheduler.sweepPendingNotifications();
        scheduler.sweepPendingNotifications();
        scheduler.sweepPendingNotifications();
        verify(notificationService, times(3)).processAllPending();
    }

    @Test
    void sweep_doesNotPropagateException_whenServiceFails() {
        doThrow(new RuntimeException("Redis unavailable"))
            .when(notificationService).processAllPending();

        // Must not throw - the scheduler must be resilient
        scheduler.sweepPendingNotifications();

        verify(notificationService, times(1)).processAllPending();
    }
}
