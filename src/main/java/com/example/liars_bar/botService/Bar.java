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

    public void execute(Group group, Player player) {
        group.getPlayers().forEach(
                p -> answerProducer.response(
                        Utils.editText(p.getId(), getResult(group, player), p.getBar())
                )
        );
    }

    public void executeAll(Group group, String text) {
        group.getPlayers().forEach(
                p -> answerProducer.response(
                        Utils.editText(p.getId(), text, p.getBar())
                )
        );
    }

    public String getResult(Group group, Player pTemp) {

        StringBuilder text = new StringBuilder("\uD83C\uDCCF : " + group.getCard() + "\n" );

        for (Player p: group.getPlayers()) {
            text.append(getANC(p))
                    .append(" - (" )
                    .append(p.getAttempt())
                    .append("/6) " )
                    .append(getA(p, pTemp))
                    .append(" | ♠️x")
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

    private String getA(Player p, Player playerTemp) {
        return p.equals(playerTemp) ? "\uD83D\uDC7E" : "";
    }

    private String getANC(Player p) {
        return (p.getIsAlive() ? "\uD83D\uDC64 : " : "💀 : ") + p.getName();
    }

}
