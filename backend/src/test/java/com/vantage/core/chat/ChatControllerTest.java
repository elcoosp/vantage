package com.vantage.core.chat;

import com.vantage.core.chat.app.ChatService;
import com.vantage.core.chat.ui.ChatController;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ChatControllerTest {

    @Test
    void should_stream_chunks_when_calling_chat_endpoint() throws Exception {
        ChatService chatService = mock(ChatService.class);
        when(chatService.getResponseWords("hello")).thenReturn(List.of("Hello", "world", "!"));

        ChatController controller = new ChatController(chatService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        MvcResult result = mockMvc.perform(get("/api/v1/chat/stream")
                .param("query", "hello")
                .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertThat(content).contains("data: Hello");
        assertThat(content).contains("data: world");
        assertThat(content).contains("data: !");
    }
}
