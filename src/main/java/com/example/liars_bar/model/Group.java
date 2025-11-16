package com.example.liars_bar.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity(name = "_group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn(name = "player_index")
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    private Character Card;
    private Character lastCard;
    private Boolean isLie = false;
    private int playerCount;
    private int turn = 0;
}
