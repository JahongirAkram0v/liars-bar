package com.example.liars_bar.rabbitmqService;

import com.example.liars_bar.model.Request;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;

import static com.example.liars_bar.config.RabbitQueue.REQUEST_QUEUE;

@Service
@RequiredArgsConstructor
public class Producer {

    private final RabbitTemplate rabbitTemplate;

    public void request(Request request) {
        rabbitTemplate.convertAndSend(REQUEST_QUEUE, request);
    }

}
