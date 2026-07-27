package com.vantage.core.chat;

import com.vantage.core.chat.app.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ChatControllerIT {

    @LocalServerPort
    private int port;

    @MockitoBean
    private ChatService chatService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void should_stream_chunks_when_calling_chat_endpoint() {
        when(chatService.getResponseWords("hello")).thenReturn(List.of("Hello", "world", "!"));

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/chat/stream?query=hello",
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).contains("data: Hello");
        assertThat(body).contains("data: world");
        assertThat(body).contains("data: !");
    }
}
