package com.example.liars_bar.telegram;

import com.example.liars_bar.config.TelegramProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Bot API'ga sinxron so'rov yuboradi.
 */
@Component
public class TelegramApi {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper;
    private final String baseUrl;

    public TelegramApi(TelegramProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.baseUrl = properties.apiUrl() + "/bot" + properties.botToken() + "/";
    }

    public JsonNode call(String method, Map<String, Object> body) {
        return call(method, body, TIMEOUT);
    }

    /** @param timeout long polling uchun Telegram kutish vaqtidan uzunroq bo'lishi kerak */
    public JsonNode call(String method, Map<String, Object> body, Duration timeout) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(baseUrl + method))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize request for " + method, e);
        }

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Istisno xabarida URL (token) bo'lishi mumkin, shuning uchun faqat turini olamiz
            throw new TelegramException(TelegramException.NETWORK_ERROR, e.getClass().getSimpleName(), 0);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramException(TelegramException.NETWORK_ERROR, "interrupted", 0);
        }

        JsonNode node;
        try {
            node = mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new TelegramException(response.statusCode(), "invalid response", 0);
        }
        if (node.path("ok").asBoolean(false)) {
            return node.path("result");
        }
        throw new TelegramException(
                node.path("error_code").asInt(response.statusCode()),
                node.path("description").asText(""),
                node.path("parameters").path("retry_after").asInt(0)
        );
    }
}
