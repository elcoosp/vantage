package com.vantage.core.developerlog.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory ring buffer of recent API requests plus the set of live SSE
 * subscribers. {@code RequestLogFilter} feeds it; the developer portal's
 * {@code GET /api/v1/developer/logs/stream} consumes it.
 */
@Component
public class RequestLogBuffer {

    private static final Logger log = LoggerFactory.getLogger(RequestLogBuffer.class);
    private static final int MAX_HISTORY = 500;
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Deque<RequestLog> history = new ArrayDeque<>();
    private final List<SseEmitter> subscribers = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper;

    public RequestLogBuffer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void record(RequestLog entry) {
        if (history.size() >= MAX_HISTORY) {
            history.pollFirst();
        }
        history.addLast(entry);
        broadcast(entry);
    }

    public synchronized List<RequestLog> recent(int limit) {
        return history.stream()
                .skip(Math.max(0, history.size() - limit))
                .toList();
    }

    /**
     * Opens an SSE stream, replaying the last {@code replay} entries before
     * switching to live delivery.
     */
    public SseEmitter subscribe(int replay) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        subscribers.add(emitter);
        Runnable cleanup = () -> subscribers.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());
        try {
            for (RequestLog entry : recent(replay)) {
                send(emitter, entry);
            }
        } catch (Exception e) {
            log.warn("Failed to replay request log history: {}", e.getMessage());
        }
        return emitter;
    }

    private void broadcast(RequestLog entry) {
        for (SseEmitter subscriber : subscribers) {
            try {
                send(subscriber, entry);
            } catch (Exception e) {
                subscribers.remove(subscriber);
            }
        }
    }

    private void send(SseEmitter emitter, RequestLog entry) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of(
                "id", entry.id().toString(),
                "method", entry.method(),
                "path", entry.path(),
                "status", entry.status(),
                "durationMs", entry.durationMs(),
                "at", entry.at().toString()));
        emitter.send(SseEmitter.event().name("log").data(json));
    }
}
