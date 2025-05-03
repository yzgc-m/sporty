package com.sporty.live_events.controller;

import com.sporty.live_events.service.LiveEventTrackingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class LiveEventTrackingController {

    private final LiveEventTrackingService liveEventTrackingService;

    public LiveEventTrackingController(LiveEventTrackingService liveEventTrackingService) {
        this.liveEventTrackingService = liveEventTrackingService;
    }

    @PostMapping("/status")
    void scheduleTracker(@RequestBody @Valid LiveEventTrackingRequest liveEventTrackingRequest) {
        liveEventTrackingService.scheduleTracker(liveEventTrackingRequest.eventId(), liveEventTrackingRequest.status());
    }
}
