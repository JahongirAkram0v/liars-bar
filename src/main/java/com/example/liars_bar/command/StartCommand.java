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
        String text = "O'yinchilar sonini tanlang!";
        List<List<Map<String, Object>>> keyboards = List.of(
                List.of(
                        Map.of("text", "2", "callback_data", "x2"),
                        Map.of("text", "3", "callback_data", "x3"),
                        Map.of("text", "4", "callback_data", "x4")
                )
        );
        answerProducer.response(Utils.text(id, text, keyboards));
    }
}
