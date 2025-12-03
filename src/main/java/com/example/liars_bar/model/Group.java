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
    @OrderBy("playerIndex ASC")
    @Builder.Default
    private List<Player> players = new ArrayList<>();
    private int lPI;

    @Builder.Default
    private boolean bar = false;

    private Character Card;
    @Builder.Default
    private List<Character> throwCards = new ArrayList<>();
    private int playerCount;
    @Builder.Default
    private int turn = 0;
}
