package com.sporty.live_events.service.scheduler;

import com.sporty.live_events.service.external.ExternalLiveScoreRestApiService;
import com.sporty.live_events.service.kafka.LiveScoreKafkaMessage;
import com.sporty.live_events.service.kafka.LiveScoreKafkaPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Service
public class LiveScoreTaskSchedulerServiceBean implements LiveScoreTaskSchedulerService {
    private static final Logger log = LoggerFactory.getLogger(LiveScoreTaskSchedulerServiceBean.class);
    private final static int TASK_CYCLE_DURATION_IN_SEC = 10;

    private final ExternalLiveScoreRestApiService externalLiveScoreRestApiService;
    private final LiveScoreKafkaPublisherService liveScoreKafkaPublisherService;
    private final TaskScheduler taskScheduler;

    public LiveScoreTaskSchedulerServiceBean(ExternalLiveScoreRestApiService externalLiveScoreRestApiService, LiveScoreKafkaPublisherService liveScoreKafkaPublisherService, TaskScheduler taskScheduler) {
        this.externalLiveScoreRestApiService = externalLiveScoreRestApiService;
        this.liveScoreKafkaPublisherService = liveScoreKafkaPublisherService;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public ScheduledFuture<?> scheduleJob(long eventId) {
        return taskScheduler.scheduleAtFixedRate(getTask(eventId), Duration.ofSeconds(TASK_CYCLE_DURATION_IN_SEC));
    }

    private Runnable getTask(long eventId) {
        return () -> {
            try {
                var response = externalLiveScoreRestApiService.queryCurrentScore(eventId);
                var message = new LiveScoreKafkaMessage(response.eventId(), response.currentScore());

                liveScoreKafkaPublisherService.publishMessage(message);
            } catch (Exception exc) {
                log.error("Could not publish for event {} for this cycle", eventId, exc);
                throw new LiveScoreTaskSchedulerException("Failed to publish live score for event " + eventId, exc);
            }
        };
    }

    @Override
    public void unscheduleJob(ScheduledFuture<?> job) {
        job.cancel(false);
    }
}
