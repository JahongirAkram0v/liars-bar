package com.example.liars_bar.rabbitmqService;

import com.example.liars_bar.model.Request;
import com.example.liars_bar.model.RequestCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import static com.example.liars_bar.config.RabbitQueue.*;

@Service
@RequiredArgsConstructor
public class Producer {

    private final RabbitTemplate rabbitTemplate;

    public void requestMessage(Request request) {
        rabbitTemplate.convertAndSend(REQUEST_MESSAGE_QUEUE, request);
    }

    public void requestCallback(RequestCallback request) {
        rabbitTemplate.convertAndSend(REQUEST_CALLBACK_QUEUE, request);
    }

}
