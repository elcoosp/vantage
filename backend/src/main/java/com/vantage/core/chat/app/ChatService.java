package com.vantage.core.chat.app;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatService {
    public List<String> getResponseWords(String query) {
        // Canned response for testing
        return List.of("Hello", "world", "!");
    }
}
