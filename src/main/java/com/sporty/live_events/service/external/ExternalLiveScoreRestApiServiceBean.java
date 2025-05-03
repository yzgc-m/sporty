package com.sporty.live_events.service.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExternalLiveScoreRestApiServiceBean implements ExternalLiveScoreRestApiService {
    public static final String SCORE_BASE_URI = "http://localhost:8080/mock/status/";
    private static final Logger log = LoggerFactory.getLogger(ExternalLiveScoreRestApiServiceBean.class);
    private final RestClient restClient;

    public ExternalLiveScoreRestApiServiceBean(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public ExternalLiveScoreResponse queryCurrentScore(long eventId) {
        log.info("Querying live score for event {}", eventId);

        var response = restClient.get()
                .uri(SCORE_BASE_URI + eventId)
                .retrieve()
                .toEntity(ExternalLiveScoreResponse.class)
                .getBody();

        log.info("Live score retrieved {}", response);

        return response;
    }
}
