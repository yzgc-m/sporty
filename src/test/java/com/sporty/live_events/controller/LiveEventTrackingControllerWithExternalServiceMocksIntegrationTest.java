package com.sporty.live_events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sporty.live_events.helper.EventIdGenerator;
import com.sporty.live_events.service.LiveEventTrackingService;
import com.sporty.live_events.service.external.ExternalLiveScoreResponse;
import com.sporty.live_events.service.kafka.LiveScoreKafkaMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LiveEventTrackingControllerWithExternalServiceMocksIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiveEventTrackingService liveEventTrackingService;

    @MockitoBean
    private KafkaTemplate<String, LiveScoreKafkaMessage> kafkaTemplate;

    @MockitoBean
    private RestClient restClient;

    private final static int TASK_CYCLE_DURATION_IN_SEC = 10;

    @Test
    void shouldPublishScoreWhenEventTrackingRequested() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);
        var externalLiveScoreResponse = setupRestServiceMock(eventId, true);

        callRestEndpoint(liveEventTrackingRequest);

        var message = new LiveScoreKafkaMessage(externalLiveScoreResponse.eventId(), externalLiveScoreResponse.currentScore());
        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(kafkaTemplate).send("live_score", message)
        );
    }


    @Test
    void shouldRetryExternalRestApiAtLeastThreeTimesWhenConnectionFails() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);
        setupRestServiceMock(eventId, false);

        callRestEndpoint(liveEventTrackingRequest);

        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(restClient, atLeast(3)).get()
        );
        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(kafkaTemplate, never()).send(eq("live_score"), argThat(message ->
                        message.eventId() == eventId
                ))
        );
    }

    @Test
    void shouldRetryKafkaAtLeastThreeTimesWhenKafkaSenderFails() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);
        var externalLiveScoreResponse = setupRestServiceMock(eventId, true);
        when(kafkaTemplate.send(any(), any())).thenThrow(new RuntimeException());

        callRestEndpoint(liveEventTrackingRequest);

        var message = new LiveScoreKafkaMessage(externalLiveScoreResponse.eventId(), externalLiveScoreResponse.currentScore());
        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(kafkaTemplate, atLeast(3)).send("live_score", message)
        );
    }

    @Test
    void shouldUnscheduleJob() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        liveEventTrackingService.scheduleTracker(eventId, true);

        assertNotNull(liveEventTrackingService.getScheduledTrackerForEvent(eventId));

        setupRestServiceMock(eventId, true);
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, false);

        callRestEndpoint(liveEventTrackingRequest);

        assertNull(liveEventTrackingService.getScheduledTrackerForEvent(eventId));
    }

    @Test
    void shouldPublishLiveScoreInAtLeastTwoCycles() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);
        var externalLiveScoreResponse = setupRestServiceMock(eventId, true);

        callRestEndpoint(liveEventTrackingRequest);

        var message = new LiveScoreKafkaMessage(externalLiveScoreResponse.eventId(), externalLiveScoreResponse.currentScore());
        await().atMost(TASK_CYCLE_DURATION_IN_SEC + 5, TimeUnit.SECONDS).untilAsserted(() ->
                verify(kafkaTemplate, atLeast(2)).send("live_score", message)
        );
    }

    @Test
    void shouldSendAtSecondCycleEvenIfFirstCycleWasThrowingAnException() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);
        setupRestServiceMock(eventId, false);

        callRestEndpoint(liveEventTrackingRequest);

        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(restClient, atLeast(3)).get()
        );
        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(kafkaTemplate, never()).send(eq("live_score"), argThat(message ->
                        message.eventId() == eventId
                ))
        );

        reset(restClient);
        var externalLiveScoreResponse = setupRestServiceMock(eventId, true);

        var message = new LiveScoreKafkaMessage(eventId, externalLiveScoreResponse.currentScore());
        await().atMost(TASK_CYCLE_DURATION_IN_SEC, TimeUnit.SECONDS).untilAsserted(() ->
                verify(kafkaTemplate).send("live_score", message)
        );
    }

    @Test
    void shouldRejectInvalidIds() throws Exception {
        var eventId = EventIdGenerator.generateInvalidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);

        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liveEventTrackingRequest)))
                .andExpect(status().isBadRequest());
    }

    private void callRestEndpoint(LiveEventTrackingRequest liveEventTrackingRequest) throws Exception {
        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liveEventTrackingRequest)))
                .andExpect(status().isOk());
    }

    private ExternalLiveScoreResponse setupRestServiceMock(long eventId, boolean successful) {
        if (!successful) {
            when(restClient.get()).thenThrow(new RuntimeException());
            return null;
        }

        var externalLiveScoreResponse = new ExternalLiveScoreResponse(eventId, UUID.randomUUID().toString());

        var requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        var requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        var responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("http://localhost:8080/mock/status/" + eventId)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(ExternalLiveScoreResponse.class)).thenReturn(ResponseEntity.ok(externalLiveScoreResponse));

        return externalLiveScoreResponse;
    }
}