package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.rabbitmqService.AnswerProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Bar {

    private final AnswerProducer answerProducer;

    public void execute(Group group) {
        group.getPlayers().forEach(
                p -> answerProducer.response(
                        Utils.editText(p.getId(), getResult(group), p.getBar())
                )
        );
    }

    public void executeP(Player p, String text) {
        answerProducer.response(Utils.editText(p.getId(), text, p.getBar()));
    }

    public void executeAll(Group group, String text) {
        group.getPlayers().forEach(
                p -> answerProducer.response(
                        Utils.editText(p.getId(), text, p.getBar())
                )
        );
    }

    public String getResult(Group group) {

        StringBuilder text = new StringBuilder("\uD83D\uDD38 : " + group.getCard());
        int size = group.getThrowCards().size();
        String name = group.getPlayers().get(group.getLPI()).getName();
        if (size != 0) text.append(" ❗️").append(name).append(" \uD83C\uDCCFx").append(size);
        text.append("\n➖➖➖➖➖➖➖➖➖➖\n");
        for (Player p: group.getPlayers()) {
            text.append(getANC(p))
                    .append(" - (" )
                    .append(p.getAttempt())
                    .append("/6) " )
                    .append(getA(p.getIndex(), group.getTurn()))
                    .append(" | \uD83C\uDCCFx")
                    .append(p.getCards().size())
                    .append(getE(p))
                    .append("\n");
        }
        return text.toString().trim();
    }

    private String getE(Player p) {
        if (p.getEM() == 0) return Utils.emojis.getFirst();
        return " /" + Utils.emojis.get(p.getEM());
    }

    private String getA(int pIndex, int pTIndex) {
        return pIndex == pTIndex ? "\uD83D\uDC7E" : "";
    }

    private String getANC(Player p) {
        return (p.isAlive() ? "\uD83D\uDC64 : " : "💀 : ") + p.getName();
    }

}
