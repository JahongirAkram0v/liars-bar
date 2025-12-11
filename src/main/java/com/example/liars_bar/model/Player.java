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
    //
    @Column(length = 30)
    private String name;
    //
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PlayerState playerState = PlayerState.START;
    //
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "event_id")
    private Event event;
    //
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    private int playerIndex;

    private int chances;

    @Builder.Default
    private List<Character> cards = new ArrayList<>();

    @Builder.Default
    private List<Integer> temp = new ArrayList<>();

    @Builder.Default
    private int attempt = 0;

    @Builder.Default
    private int eM = 0;

    @Builder.Default
    private int bar = -1;

    @Builder.Default
    private int card = -1;

    @Builder.Default
    private boolean c = true;

    @Builder.Default
    private boolean isAlive = true;

    @Builder.Default
    private boolean isActive = true;

}
