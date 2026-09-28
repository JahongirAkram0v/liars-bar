package com.example.liars_bar.web;

import com.example.liars_bar.player.JdbcPlayerStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "telegram.bot-token=123:test",
        "telegram.bot-username=liars_bar_bot",
        "telegram.webhook-secret=test-secret-0123456789",
        "telegram.webhook-path=/hook",
        "app.db.path=target/test-data/liars-bar.db"
})
@AutoConfigureMockMvc
class WebhookSecurityTest {

    private static final String UPDATE = "{\"update_id\":1}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcPlayerStore players;

    @Test
    void rejectsRequestsWithoutValidSecret() throws Exception {
        mvc.perform(post("/hook").contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/hook").contentType(MediaType.APPLICATION_JSON).content(UPDATE)
                        .header(WebhookSecretFilter.SECRET_HEADER, "wrong-secret-0123456789"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsTelegramRequests() throws Exception {
        mvc.perform(post("/hook").contentType(MediaType.APPLICATION_JSON).content(UPDATE)
                        .header(WebhookSecretFilter.SECRET_HEADER, "test-secret-0123456789"))
                .andExpect(status().isOk());
    }

    @Test
    void otherPathsAreNotExposed() throws Exception {
        mvc.perform(get("/hook")).andExpect(status().is4xxClientError());
        mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isNotFound());
    }

    @Test
    void sqliteStoresPlayersAndResults() {
        players.upsert(10, "Ali");
        players.upsert(10, "Vali");
        players.upsert(11, "Soli");
        int before = players.wins(10);

        players.recordGame(List.of(10L, 11L), 10);

        assertThat(players.wins(10)).isEqualTo(before + 1);
    }
}
