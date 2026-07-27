package com.vantage.core.chat;

import com.vantage.core.chat.app.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebMvcTest(com.vantage.core.chat.ui.ChatController.class)
public class ChatControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void should_stream_chunks_when_calling_chat_endpoint() throws Exception {
        when(chatService.getResponseWords("hello")).thenReturn(List.of("Hello", "world"));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/chat/stream")
                .param("query", "hello")
                .header("Accept", "text/event-stream"))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        // Expect at least one data event
        assertThat(content).contains("data: ");
    }
}
