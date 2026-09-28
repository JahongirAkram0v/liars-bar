package com.example.liars_bar;

import com.example.liars_bar.bot.UpdatePoller;
import com.example.liars_bar.player.JdbcPlayerStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "telegram.bot-token=123:test",
        "telegram.bot-username=liars_bar_bot",
        "telegram.polling-enabled=false",
        "app.db.path=target/test-data/liars-bar.db"
})
class ApplicationTest {

    @Autowired
    private JdbcPlayerStore players;

    @Autowired
    private UpdatePoller poller;

    @Test
    void startsWithoutWebServerAndPollingCanBeDisabled() {
        assertThat(poller.isRunning()).isFalse();
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
