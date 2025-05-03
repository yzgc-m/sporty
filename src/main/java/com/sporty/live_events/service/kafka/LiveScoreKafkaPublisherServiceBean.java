package com.sporty.live_events.service.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class LiveScoreKafkaPublisherServiceBean implements LiveScoreKafkaPublisherService {
    private static final Logger log = LoggerFactory.getLogger(LiveScoreKafkaPublisherServiceBean.class);

    private static final String TOPIC = "live_score";

    private final KafkaTemplate<String, LiveScoreKafkaMessage> kafkaTemplate;

    public LiveScoreKafkaPublisherServiceBean(KafkaTemplate<String, LiveScoreKafkaMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Retryable(
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public void publishMessage(LiveScoreKafkaMessage message) throws ExecutionException, InterruptedException {
        log.info("Sending kafka message for live score {}", message);

        try {
            kafkaTemplate.send(TOPIC, message);
            log.info("Kafka message published for live score {}", message);
        } catch (Exception exc) {
            log.error("Exception during kafka publish {}", message, exc);
            throw exc;
        }
    }

}
