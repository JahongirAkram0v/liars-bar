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
@Table(name = "_group")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("index ASC")
    @Builder.Default
    private List<Player> players = new ArrayList<>();
    private int lPI;

    private Character Card;
    @Builder.Default
    private List<Character> throwCards = new ArrayList<>();
    private int pC;
    @Builder.Default
    private int turn = 0;
}
