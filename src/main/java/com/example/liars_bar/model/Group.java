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
    @MapKey(name = "id")
    @Builder.Default
    private Map<Long, Player> players = new HashMap<>();

    @Builder.Default
    private Long lI = -1L;

    private Character card;

    @Builder.Default
    private List<Character> throwCards = new ArrayList<>();

    private int pC;

    @Builder.Default
    private Long turn = 0L;

    public List<Player> getPlayersList() {
        return players.values().stream()
                .sorted(Comparator.comparing(Player::getId))
                .toList();
    }

    public List<Long> getPlayersListIndex() {
        return players.keySet().stream()
                .sorted()
                .toList();
    }

    public Optional<Player> currentPlayer() {
        Player player = players.get(turn);
        if (player == null) {
            System.err.println(turn+"\n"+this.getPlayersListIndex());
            System.err.println("============");
        }
        return Optional.ofNullable(player);
    }

    public Player getPlayer(Long index) {
        return players.get(index);
    }

    public int playerCount() {
        return players.size();
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        player.setGroup(this);
    }

    public void removePlayer(Long id) {
        Player player = players.remove(id);
        if (player != null) {
            player.setGroup(null);
        }
    }
}
