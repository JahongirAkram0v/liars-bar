package com.example.liars_bar.command;

import com.example.liars_bar.botService.Utils;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StartCommand {

    private final AnswerProducer answerProducer;


    public void execute(Long id) {
        String text = "Welcome to the Liars' Bar! Choose an option below to get started:";
        List<List<Map<String, Object>>> keyboards = List.of(
                List.of(
                        Map.of("text", "2", "callback_data", "c2"),
                        Map.of("text", "3", "callback_data", "c3"),
                        Map.of("text", "4", "callback_data", "c4")
                )
        );
        answerProducer.response(Utils.text(id, text, keyboards));
    }
}
