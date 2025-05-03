package com.sporty.live_events.service.external;

public interface ExternalLiveScoreRestApiService {

    ExternalLiveScoreResponse queryCurrentScore(long eventId);
}
