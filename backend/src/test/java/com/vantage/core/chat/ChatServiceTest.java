package com.vantage.core.chat;

import com.vantage.core.chat.app.ChatService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ChatServiceTest {

    private final ChatService chatService = new ChatService();

    @Test
    void should_return_canned_response_for_any_query() {
        List<String> words = chatService.getResponseWords("anything");
        assertThat(words).containsExactly("Hello", "world", "!");
    }
}
