package com.example.liars_bar.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.liars_bar.config.RabbitQueue.*;

@Configuration
public class RabbitmqConfig {

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue requestMessageQueue() {
        return new Queue(REQUEST_MESSAGE_QUEUE);
    }

    @Bean
    public Queue requestCallbackQueue() {
        return new Queue(REQUEST_CALLBACK_QUEUE);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(RESPONSE_QUEUE);
    }

}
