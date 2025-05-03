package com.sporty.live_events.service;

import com.sporty.live_events.service.scheduler.LiveScoreTaskSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Service
public class LiveEventTrackingServiceBean implements LiveEventTrackingService {

    private static final Logger log = LoggerFactory.getLogger(LiveEventTrackingServiceBean.class);

    private final LiveScoreTaskSchedulerService liveScoreTaskSchedulerService;

    private final Map<Long, ScheduledFuture<?>> scheduledJobs;

    public LiveEventTrackingServiceBean(LiveScoreTaskSchedulerService liveScoreTaskSchedulerService) {
        this.liveScoreTaskSchedulerService = liveScoreTaskSchedulerService;

        this.scheduledJobs = new ConcurrentHashMap<>();
    }

    @Override
    public void scheduleTracker(long eventId, boolean status) {
        if (status) {
            schedule(eventId);
        } else {
            unSchedule(eventId);
        }
    }

    @Override
    public ScheduledFuture<?> getScheduledTrackerForEvent(long eventId) {
        return scheduledJobs.get(eventId);
    }

    private void schedule(long eventId) {
        if (scheduledJobs.containsKey(eventId)) return;

        log.info("Scheduling live score tracker for event {}", eventId);
        var future = liveScoreTaskSchedulerService.scheduleJob(eventId);
        scheduledJobs.put(eventId, future);
    }

    private void unSchedule(long eventId) {
        if (!scheduledJobs.containsKey(eventId)) return;

        log.info("Unscheduling live score tracker for event {}", eventId);
        liveScoreTaskSchedulerService.unscheduleJob(scheduledJobs.get(eventId));
        scheduledJobs.remove(eventId);
    }
}
