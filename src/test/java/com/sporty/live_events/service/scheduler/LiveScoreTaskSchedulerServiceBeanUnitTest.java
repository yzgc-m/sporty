package com.sporty.live_events.service.scheduler;

import com.sporty.live_events.service.external.ExternalLiveScoreRestApiService;
import com.sporty.live_events.service.kafka.LiveScoreKafkaPublisherService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveScoreTaskSchedulerServiceBeanUnitTest {

    private final ExternalLiveScoreRestApiService externalLiveScoreRestApiService = mock();
    private final LiveScoreKafkaPublisherService liveScoreKafkaPublisherService = mock();
    private final TaskScheduler taskScheduler = mock();

    private final LiveScoreTaskSchedulerService schedulerService =
            new LiveScoreTaskSchedulerServiceBean(externalLiveScoreRestApiService, liveScoreKafkaPublisherService, taskScheduler);

    @Test
    void shouldThrowExceptionWhenExternalServiceFails() {
        var eventId = 1234L;
        when(externalLiveScoreRestApiService.queryCurrentScore(eventId))
                .thenThrow(new RuntimeException("External service failed"));

        var runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        when(taskScheduler.scheduleAtFixedRate(runnableCaptor.capture(), any(Duration.class)))
                .thenReturn(mock());

        schedulerService.scheduleJob(eventId);

        Runnable capturedTask = runnableCaptor.getValue();
        assertThrows(LiveScoreTaskSchedulerException.class, capturedTask::run);
    }
}