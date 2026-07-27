package com.vantage.core.chat.ui;

import com.vantage.core.chat.app.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String query) {
        log.info("Chat stream started for query: {}", query);
        SseEmitter emitter = new SseEmitter(300000L);

        emitter.onCompletion(() -> log.info("Chat stream completed for query: {}", query));
        emitter.onTimeout(() -> {
            log.warn("Chat stream timed out for query: {}", query);
            emitter.complete();
        });

        Thread.startVirtualThread(() -> {
            try {
                List<String> words = chatService.getResponseWords(query);
                for (String word : words) {
                    emitter.send(SseEmitter.event().data(word));
                    Thread.sleep(50);
                }
                emitter.complete();
                log.info("Chat stream finished successfully for query: {}", query);
            } catch (IOException e) {
                log.error("IO error during chat stream", e);
                emitter.completeWithError(e);
            } catch (InterruptedException e) {
                log.warn("Chat stream interrupted for query: {}", query);
                Thread.currentThread().interrupt();
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
