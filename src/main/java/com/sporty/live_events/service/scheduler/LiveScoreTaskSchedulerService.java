package com.sporty.live_events.service.scheduler;

import java.util.concurrent.ScheduledFuture;

public interface LiveScoreTaskSchedulerService {
    ScheduledFuture<?> scheduleJob(long eventId);

    void unscheduleJob(ScheduledFuture<?> job);
}
