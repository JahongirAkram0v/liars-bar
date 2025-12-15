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
@Entity
@Table(name = "player")
public class Player {

    @Id
    private Long id;

    private int index;

    @Column(length = 30)
    private String name;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlayerState playerState = PlayerState.START;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private int chances;

    @Builder.Default
    private List<Character> cards = new ArrayList<>();

    @Builder.Default
    private List<Integer> temp = new ArrayList<>();

    @Builder.Default
    private int attempt = 0;

    @Builder.Default
    private int eM = 0;

    private int bar;

    private int card;

    @Builder.Default
    private int sticker = -1;

    @Builder.Default
    private boolean isAlive = true;

    @Builder.Default
    private boolean isActive = true;
}
