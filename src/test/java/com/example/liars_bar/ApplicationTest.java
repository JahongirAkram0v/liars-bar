package com.example.liars_bar;

import com.example.liars_bar.bot.UpdatePoller;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "telegram.bot-token=123:test",
        "telegram.bot-username=liars_bar_bot",
        "telegram.polling-enabled=false"
})
class ApplicationTest {

    @Autowired
    private UpdatePoller poller;

    @Test
    void startsWithoutWebServerAndPollingCanBeDisabled() {
        assertThat(poller.isRunning()).isFalse();
    }
}
