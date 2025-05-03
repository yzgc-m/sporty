package com.sporty.live_events.service.kafka;

import java.util.concurrent.ExecutionException;

public interface LiveScoreKafkaPublisherService {
    void publishMessage(LiveScoreKafkaMessage message) throws ExecutionException, InterruptedException;
}
