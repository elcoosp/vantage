package com.vantage.core.chat.ui;

import com.vantage.core.chat.app.ChatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
        emitter.complete();
        return emitter;
    }
}
