package com.sporty.live_events.mock;

import com.sporty.live_events.service.kafka.LiveScoreKafkaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class KafkaConsumerService {
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @KafkaListener(topics = "live_score", groupId = "live_score_group")
    public void listen(LiveScoreKafkaMessage message) {
        log.info("Received kafka message: {}", message);
    }
}
