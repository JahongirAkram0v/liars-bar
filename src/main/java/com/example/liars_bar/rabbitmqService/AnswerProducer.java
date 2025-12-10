package com.example.liars_bar.rabbitmqService;

import com.example.liars_bar.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static com.example.liars_bar.config.RabbitQueue.RESPONSE_QUEUE;

@Service
@RequiredArgsConstructor
public class AnswerProducer {

    private final RabbitTemplate rabbitTemplate;

    public void response(Response response) {
        rabbitTemplate.convertAndSend(RESPONSE_QUEUE, response);
    }
}
