package com.example.liars_bar.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Player {

    @Id
    private Long id;
    private int playerIndex;
    private String name;
    private int chances;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlayerState playerState = PlayerState.START;

    @OrderColumn(name = "cards_index")
    @Builder.Default
    private List<Character> cards = new ArrayList<>();

    @Builder.Default
    private List<Integer> temp = new ArrayList<>();

    @Builder.Default
    private int attempt = 0;

    @Builder.Default
    private Boolean isAlive = true;

    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;
}
