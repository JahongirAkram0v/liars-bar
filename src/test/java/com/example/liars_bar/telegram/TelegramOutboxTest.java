package com.example.liars_bar.telegram;

import com.example.liars_bar.config.TelegramProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramOutboxTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CountDownLatch release = new CountDownLatch(1);
    private final List<String> calls = new CopyOnWriteArrayList<>();

    /** Birinchi so'rovni ushlab turadi, shu vaqtda keyingilari navbatda to'planadi. */
    private final TelegramApi api = new TelegramApi(new TelegramProperties(
            "1:t", "liars_bar_bot", "/hook", "secret-0123456789abc", "https://api.telegram.org", null), mapper) {
        @Override
        public JsonNode call(String method, Map<String, Object> body) {
            calls.add(method + ":" + body.get("text"));
            if (calls.size() == 1) {
                await(release);
            }
            return mapper.createObjectNode().put("message_id", 1);
        }
    };
    private final TelegramOutbox outbox = new TelegramOutbox(api);

    @AfterEach
    void tearDown() {
        outbox.close();
    }

    @Test
    void queuedEditsOfSameMessageAreMergedIntoLatest() throws Exception {
        outbox.send(7, "first", null);
        for (int i = 1; i <= 5; i++) {
            outbox.edit(7, 50, "table " + i, null);
        }
        outbox.edit(7, 51, "hand", null);
        release.countDown();

        waitForCalls(3);
        assertThat(calls).containsExactly(
                "sendMessage:first",
                "editMessageText:table 5",
                "editMessageText:hand");
    }

    @Test
    void callbackAnswersDoNotWaitForTheChatQueue() throws Exception {
        outbox.send(7, "blocked", null);
        outbox.answerCallback(7, "q1", null);

        waitForCalls(2);
        assertThat(calls).contains("answerCallbackQuery:null");
        assertThat(release.getCount()).isEqualTo(1); // birinchi so'rov hali tugamagan
        release.countDown();
    }

    private void waitForCalls(int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 3_000;
        while (calls.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        Thread.sleep(50);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
