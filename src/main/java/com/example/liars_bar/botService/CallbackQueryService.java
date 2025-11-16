package com.example.liars_bar.botService;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.service.GroupService;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

import static com.example.liars_bar.model.PlayerState.ADD;

@Component
@RequiredArgsConstructor
public class CallbackQueryService {

    private final Dotenv dotenv = Dotenv.load();
    private final String botUsername = dotenv.get("TELEGRAM_BOT_USERNAME");

    private final GroupService groupService;
    private final SendService sendService;
    private final ShuffleService shuffleService;

    public void check(String callbackData, Integer messageId, Player player, String callbackQueryId) {

        if (callbackData.startsWith("c ")) {
            int count = callbackData.charAt(2) - '0';
            Group group = new Group();
            group.setPlayerCount(count);
            groupService.save(group);

            sendService.send(
                    MessageUtilsService.editMessage(
                            messageId,
                            player.getId(),
                            "O'yinni boshlash uchun referral!\n" +
                            "https://t.me/" + botUsername + "?start=" + group.getId()
                    ),
                    "editMessageText"
            );
        }
        else if (callbackData.equals("b")) {

            Group group = player.getGroup();
            int playersSize = group.getPlayers().size();
            System.out.println(playersSize);

            if (playersSize < group.getPlayerCount()) {
                player.setChances(new Random().nextInt(6) + 1);
                player.setPlayerState(ADD);
                player.setMessageId(messageId);
                player.setGroup(group);
                group.getPlayers().add(player);
                groupService.save(group);

                if (playersSize + 1 == group.getPlayerCount()) {
                    shuffleService.shuffle(group);
                }
            }
            else {
                sendService.send(
                        MessageUtilsService.sendMessage(
                                player.getId(),
                                "Guruh to'lgan. Yangi guruh yaratish uchun /start buyrug'ini bosing yoki boshqa guruhga qo'shiling."
                        ),
                        "sendMessage"
                );
            }
        }
        else if (callbackData.equals("l")) {

            //yangilayman

            Group group = player.getGroup();

            if (group.getLastCard() == null) {
                sendService.send(
                        MessageUtilsService.errorMessage(callbackQueryId),
                        "answerCallbackQuery"
                );
                return;
            }

            group.getPlayers().forEach(
                    p -> sendService.send(
                            MessageUtilsService.sendMessage(
                                    p.getId(),
                                    player.getName() + " ishonmadi.\n" + group.getLastCard() + " ni tashlagan ekan."
                            ),
                            "sendMessage"
                    )
            );

            if (group.getIsLie()) {
                Player playerTemp = group.getPlayers().get(group.getTurn());

                if (playerTemp.getChances() == 1) {
                    playerTemp.setIsAlive(false);
                    sendService.send(
                            MessageUtilsService.sendMessage(
                                    playerTemp.getId(),
                                    "Sizda imkoniyat qolmadi. Siz o'yindan chiqarildingiz."
                            ),
                            "sendMessage"
                    );
                    group.getPlayers().forEach(
                            p -> sendService.send(
                                    MessageUtilsService.sendMessage(
                                            p.getId(),
                                            playerTemp.getName() + " o'yindan chiqarildi."
                                    ),
                                    "sendMessage"
                            )
                    );
                }

            }
        }
        else if (callbackData.equals("t")) {
            //yangilayman
        }
        else {

        }

    }
}
