package com.example.liars_bar.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "_group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @MapKey(name = "index")
    @Builder.Default
    private Map<Integer, Player> players = new HashMap<>();

    @Builder.Default
    private int lI = -1;

    private Character card;

    @Builder.Default
    private List<Character> throwCards = new ArrayList<>();

    private int pC;

    @Builder.Default
    private int turn = 0;

    public List<Player> getPlayersList() {
        return new ArrayList<>(players.values());
    }

    public List<Integer> getPlayersListIndex() {
        return new ArrayList<>(players.keySet());
    }

    public Player currentPlayer() {
        return players.get(turn);
    }

    public Player getPlayer(int index) {
        return players.get(index);
    }

    public int playerCount() {
        return players.size();
    }

    public void addPlayer(Player player) {
        players.put(player.getIndex(), player);
        player.setGroup(this);
    }

    public void removePlayer(int index) {
        Player player = players.remove(index);
        if (player != null) {
            player.setGroup(null);
        }
    }
}
