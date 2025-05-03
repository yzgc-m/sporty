package com.sporty.live_events.service;

import java.util.concurrent.ScheduledFuture;

public interface LiveEventTrackingService {
    void scheduleTracker(long eventId, boolean status);

    ScheduledFuture<?> getScheduledTrackerForEvent(long eventId);
}
