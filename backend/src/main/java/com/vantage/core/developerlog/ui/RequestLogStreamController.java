package com.vantage.core.developerlog.ui;

import com.vantage.core.developerlog.app.RequestLog;
import com.vantage.core.developerlog.app.RequestLogBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Live API request log stream for the developer portal.
 *
 * <p>Served as Server-Sent Events ({@code text/event-stream}); each event is
 * named {@code log} and carries one JSON-encoded {@code RequestLog}. The
 * browser connects with {@code EventSource}, which cannot send custom
 * headers, so authentication accepts the tenant JWT via the
 * {@code access_token} query parameter (supported by
 * {@code TenantSecurityFilter}) or the regular Authorization header.</p>
 */
@RestController
@RequestMapping("/api/v1/developer")
public class RequestLogStreamController {

    private final RequestLogBuffer buffer;

    public RequestLogStreamController(RequestLogBuffer buffer) {
        this.buffer = buffer;
    }

    @GetMapping(value = "/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(name = "replay", defaultValue = "50") int replay) {
        return buffer.subscribe(Math.max(0, Math.min(replay, 500)));
    }

    /**
     * Paged snapshot of the most recent requests (newest first) — the
     * non-streaming counterpart to {@link #stream}.
     */
    @GetMapping(value = "/logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> snapshot(@RequestParam(name = "size", defaultValue = "50") int size) {
        int capped = Math.max(1, Math.min(size, 500));
        List<RequestLog> recent = buffer.recent(capped);
        List<Map<String, Object>> content = recent.stream()
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", entry.id().toString());
                    row.put("method", entry.method());
                    row.put("path", entry.path());
                    row.put("status", entry.status());
                    row.put("durationMs", entry.durationMs());
                    row.put("at", entry.at().toString());
                    return row;
                })
                .toList();
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("content", content);
        page.put("totalElements", content.size());
        return page;
    }
}
