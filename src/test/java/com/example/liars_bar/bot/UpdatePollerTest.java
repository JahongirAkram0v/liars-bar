package com.example.liars_bar.bot;

import com.example.liars_bar.config.TelegramProperties;
import com.example.liars_bar.telegram.TelegramApi;
import com.example.liars_bar.telegram.Update;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UpdatePollerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TelegramProperties properties = new TelegramProperties(
            "1:t", "liars_bar_bot", "https://api.telegram.org", true, null);

    private final List<Map<String, Object>> getUpdatesCalls = new ArrayList<>();
    private final List<Duration> timeouts = new ArrayList<>();
    private final List<Update> routed = new ArrayList<>();

    private final TelegramApi api = new TelegramApi(properties, mapper) {
        @Override
        public JsonNode call(String method, Map<String, Object> body, Duration timeout) {
            getUpdatesCalls.add(body);
            timeouts.add(timeout);
            try {
                return getUpdatesCalls.size() == 1
                        ? mapper.readTree("""
                        [
                          {"update_id": 10, "message": {"message_id": 1, "text": "/start",
                            "from": {"id": 5, "is_bot": false, "first_name": "Ali"},
                            "chat": {"id": 5, "type": "private"}}},
                          {"update_id": 11, "callback_query": {"id": "q", "data": "x2",
                            "from": {"id": 6, "is_bot": false, "first_name": "Vali"}}},
                          {"update_id": 12, "message": {"message_id": "not-a-number"}}
                        ]
                        """)
                        : mapper.createArrayNode();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    };

    private final UpdateRouter router = new UpdateRouter(null, null, null) {
        @Override
        public void submit(Update update) {
            routed.add(update);
        }
    };

    private final UpdatePoller poller = new UpdatePoller(api, router, mapper, properties);

    @AfterEach
    void tearDown() {
        router.close();
    }

    @Test
    void routesUpdatesAndAcknowledgesThemWithOffset() {
        assertThat(poller.pollOnce()).isEqualTo(2);
        poller.pollOnce();

        assertThat(routed).extracting(Update::updateId).containsExactly(10L, 11L);
        assertThat(routed.get(0).message().text()).isEqualTo("/start");
        assertThat(routed.get(1).callbackQuery().data()).isEqualTo("x2");

        assertThat(getUpdatesCalls.get(0)).containsEntry("offset", 0L)
                .containsEntry("timeout", UpdatePoller.POLL_TIMEOUT_SECONDS)
                .containsEntry("allowed_updates", List.of("message", "callback_query"));
        // yaroqsiz update (12) ham tasdiqlanadi, aks holda u qayta-qayta kelaveradi
        assertThat(getUpdatesCalls.get(1)).containsEntry("offset", 13L);
        assertThat(timeouts.get(0)).isGreaterThan(Duration.ofSeconds(UpdatePoller.POLL_TIMEOUT_SECONDS));
    }
}
