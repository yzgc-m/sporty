package com.sporty.live_events.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.sporty.live_events.helper.EventIdGenerator;
import com.sporty.live_events.service.external.ExternalLiveScoreResponse;
import com.sporty.live_events.service.kafka.LiveScoreKafkaMessage;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.EnableWireMock;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, topics = {"live_score"})
@EnableWireMock
class LiveEventTrackingControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private WireMockServer wireMockServer;

    private Consumer<String, LiveScoreKafkaMessage> consumer;

    private final static int TASK_CYCLE_DURATION_IN_SEC = 10;

    @BeforeEach
    void setup() {
        setupWireMock();
        setupTestConsumer();
    }

    @AfterEach
    void teardown() {
        consumer.close();
        wireMockServer.stop();
    }

    @Test
    void shouldPublishScoreWhenEventTrackingRequested() throws Exception {
        var eventId = EventIdGenerator.generateValidEventId();
        var liveEventTrackingRequest = new LiveEventTrackingRequest(eventId, true);
        var externalLiveScoreResponse = new ExternalLiveScoreResponse(eventId, UUID.randomUUID().toString());

        stubFor(get(urlEqualTo("/mock/status/" + eventId))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(externalLiveScoreResponse))
                        .withStatus(200)));

        mockMvc.perform(post("/api/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(liveEventTrackingRequest))
                )
                .andExpect(status().isOk());

        var record = KafkaTestUtils.getSingleRecord(consumer, "live_score", Duration.ofSeconds(TASK_CYCLE_DURATION_IN_SEC));
        var receivedMessage = record.value();

        assertThat(receivedMessage.eventId()).isEqualTo(externalLiveScoreResponse.eventId());
        assertThat(receivedMessage.currentScore()).isEqualTo(externalLiveScoreResponse.currentScore());
    }

    private void setupTestConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put("value.deserializer", "org.springframework.kafka.support.serializer.JsonDeserializer");
        consumerProps.put("spring.json.trusted.packages", "*");

        consumer = new DefaultKafkaConsumerFactory<String, LiveScoreKafkaMessage>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(LiveScoreKafkaMessage.class))
                .createConsumer();
        consumer.subscribe(Collections.singletonList("live_score"));
    }

    private void setupWireMock() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        configureFor("localhost", 8080);
    }
}