package com.sporty.live_events.mock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/mock/status")
public class MockLiveEventStatusEndpoint {
    @GetMapping("/{eventId}")
    MockLiveStatusResponse getLiveEventScore(@PathVariable long eventId) {
        return new MockLiveStatusResponse(eventId, UUID.randomUUID().toString());
    }
}
