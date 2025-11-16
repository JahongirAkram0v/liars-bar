package com.example.liars_bar.botService;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SendService {

    private final Dotenv dotenv = Dotenv.load();
    private final String url = dotenv.get("TELEGRAM_BASE_URL") + dotenv.get("TELEGRAM_BOT_TOKEN") + "/";
    private final RestTemplate restTemplate = new RestTemplate();

    public void send(Map<String, Object> answer, String method) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(answer, headers);

        try {
            restTemplate.postForObject(url + method, request, String.class);
        } catch (HttpClientErrorException e) {
            //TODO togirlashim kerak !!!
            System.out.println(e.getMessage() + " = " + answer);
        }
    }
}
