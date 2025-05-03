package com.sporty.live_events.service.kafka;

public record LiveScoreKafkaMessage(long eventId, String currentScore) {
}
