package com.sporty.live_events.service;

import com.sporty.live_events.service.scheduler.LiveScoreTaskSchedulerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class LiveEventTrackingServiceBeanUnitTest {

    private final LiveScoreTaskSchedulerService liveScoreTaskSchedulerService = mock();

    private final LiveEventTrackingService liveEventTrackingService = new LiveEventTrackingServiceBean(liveScoreTaskSchedulerService);

    @Test
    void shouldScheduleJob() {
        var eventId = 1234L;
        when(liveScoreTaskSchedulerService.scheduleJob(eventId)).thenReturn(mock());

        liveEventTrackingService.scheduleTracker(eventId, true);

        assertNotNull(liveEventTrackingService.getScheduledTrackerForEvent(eventId));
    }

    @Test
    void shouldUnscheduleJob() {
        var eventId = 1234L;
        when(liveScoreTaskSchedulerService.scheduleJob(eventId)).thenReturn(mock());

        liveEventTrackingService.scheduleTracker(eventId, true);
        liveEventTrackingService.scheduleTracker(eventId, false);

        assertNull(liveEventTrackingService.getScheduledTrackerForEvent(eventId));
    }

    @Test
    void shouldNotScheduleSameEventTwice() {
        var eventId = 1234L;
        when(liveScoreTaskSchedulerService.scheduleJob(eventId)).thenReturn(mock());

        liveEventTrackingService.scheduleTracker(eventId, true);
        liveEventTrackingService.scheduleTracker(eventId, true);

        verify(liveScoreTaskSchedulerService, times(1)).scheduleJob(eventId);
    }

    @Test
    void shouldIgnoreUnscheduleForNonExistentEvent() {
        var eventId = 1234L;

        liveEventTrackingService.scheduleTracker(eventId, false);

        verify(liveScoreTaskSchedulerService, never()).unscheduleJob(any());
    }
}