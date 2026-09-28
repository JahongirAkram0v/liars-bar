package com.example.liars_bar.web;

import com.example.liars_bar.bot.UpdateRouter;
import com.example.liars_bar.telegram.Update;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final UpdateRouter router;

    public WebhookController(UpdateRouter router) {
        this.router = router;
    }

    /** Update navbatga qo'yiladi va Telegram'ga darhol 200 qaytariladi. */
    @PostMapping(path = "${telegram.webhook-path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> onUpdate(@RequestBody Update update) {
        router.submit(update);
        return ResponseEntity.ok().build();
    }
}
