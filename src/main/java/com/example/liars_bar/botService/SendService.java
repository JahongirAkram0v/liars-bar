package com.example.liars_bar.botService;

import com.example.liars_bar.model.Event;
import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.model.Which;
import com.example.liars_bar.service.GroupService;
import com.example.liars_bar.service.PlayerService;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

import static com.example.liars_bar.model.Action.SHUFFLE;
import static com.example.liars_bar.model.Which.NOTHING;

@Component
@RequiredArgsConstructor
public class SendService {

    private final Dotenv dotenv = Dotenv.load();
    private final String url = dotenv.get("TELEGRAM_BASE_URL") + dotenv.get("TELEGRAM_BOT_TOKEN") + "/";
    private final RestTemplate restTemplate = new RestTemplate();
    private final PlayerService playerService;
    private final GroupService groupService;

    public void send(Map<String, Object> answer, String method, Which which) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(answer, headers);

        try {
            JsonNode response = restTemplate.postForObject(url + method, request, JsonNode.class);

            if (which == NOTHING) {
                return;
            }

            long id = -1L;
            int messageId = -1;
            if (response != null && response.has("result")) {
                id = response.get("result").get("chat").get("id").asLong();
                messageId = response.get("result").get("message_id").asInt();
            }
            if (id == -1 || messageId == -1) {
                System.err.println(id+"=!=!="+messageId);
                return;
            }
            Optional<Player> optionalPlayer = playerService.findById(id);
            if (optionalPlayer.isEmpty()) {
                System.err.println("Player not found: " + id);
                return;
            }
            Player player = optionalPlayer.get();

            switch (which) {
                case BAR -> player.setBar(messageId);
                case CARD -> {
                    player.setCard(messageId);
                    Group group = player.getGroup();
                    boolean is = group.getPlayers().values().stream()
                            .allMatch(p -> p.getCard() != -1);

                    if (is) {
                        Event event = Event.builder()
                                .action(SHUFFLE)
                                .endTime(Event.getMin())
                                .build();
                        group.setEvent(event);
                        groupService.save(group);
                    }

                }
                case STICKER -> player.setSticker(messageId);
            }
            playerService.save(player);
        } catch (HttpClientErrorException e) {
            //TODO togirlashim kerak !!!
            System.out.println(e.getMessage() + " = " + answer);
        }
    }
}
