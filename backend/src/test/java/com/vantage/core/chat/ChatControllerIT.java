package com.vantage.core.chat;

import com.vantage.core.chat.app.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(com.vantage.core.chat.ui.ChatController.class)
public class ChatControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void should_stream_chunks_when_calling_chat_endpoint() throws Exception {
        when(chatService.getResponseWords("hello")).thenReturn(List.of("Hello", "world", "!"));

        MvcResult result = mockMvc.perform(get("/api/v1/chat/stream")
                .param("query", "hello")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        // Expect at least one data event
        assertThat(content).contains("data: Hello");
    }
}
