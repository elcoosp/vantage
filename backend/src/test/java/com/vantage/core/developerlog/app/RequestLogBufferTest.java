package com.vantage.core.developerlog.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestLogBufferTest {

    private RequestLogBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = new RequestLogBuffer(new ObjectMapper());
    }

    private RequestLog entry(String path, int status) {
        return new RequestLog(UUID.randomUUID(), "GET", path, status, 12, Instant.now());
    }

    @Test
    void recent_should_cap_at_requested_limit() {
        for (int i = 0; i < 20; i++) {
            buffer.record(entry("/api/v1/path-" + i, 200));
        }
        List<RequestLog> recent = buffer.recent(5);
        assertThat(recent).hasSize(5);
        assertThat(recent.get(0).path()).isEqualTo("/api/v1/path-15");
        assertThat(recent.get(4).path()).isEqualTo("/api/v1/path-19");
    }

    @Test
    void history_should_evict_oldest_beyond_ring_buffer_capacity() {
        for (int i = 0; i < 520; i++) {
            buffer.record(entry("/api/v1/p-" + i, 200));
        }
        List<RequestLog> all = buffer.recent(500);
        assertThat(all).hasSize(500);
        assertThat(all.get(0).path()).isEqualTo("/api/v1/p-20");
    }

    @Test
    void record_should_never_throw_when_a_subscriber_is_dead() {
        buffer.subscribe(0);
        // A completed emitter's send() throws; the buffer must absorb it and
        // keep recording (the broadcast path removes the dead subscriber).
        SseEmitter dead = new SseEmitter(Long.MAX_VALUE);
        buffer.record(entry("/api/v1/after-dead", 200));
        assertThat(buffer.recent(10)).hasSize(1);
    }
}
