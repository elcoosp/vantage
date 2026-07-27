package com.vantage.core.chat.ui;

import com.vantage.core.chat.app.ChatService;
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

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String query) {
        SseEmitter emitter = new SseEmitter(300000L);

        Thread.startVirtualThread(() -> {
            try {
                List<String> words = chatService.getResponseWords(query);
                for (String word : words) {
                    // Send word as SSE data event
                    emitter.send(SseEmitter.event().data(word));
                    Thread.sleep(50); // simulate token generation latency
                }
                emitter.complete();
            } catch (IOException | InterruptedException e) {
                emitter.completeWithError(e);
                Thread.currentThread().interrupt();
            }
        });

        return emitter;
    }
}
