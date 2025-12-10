package com.example.liars_bar.rabbitmqService;

import com.example.liars_bar.botService.SendService;
import com.example.liars_bar.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import static com.example.liars_bar.config.RabbitQueue.RESPONSE_QUEUE;

@Service
@RequiredArgsConstructor
public class AnswerConsumer {

    private final SendService sendService;

    @RabbitListener(queues = RESPONSE_QUEUE)
    public void answerChat(Response response) {
        sendService.send(response.body(), response.method());
    }
}
