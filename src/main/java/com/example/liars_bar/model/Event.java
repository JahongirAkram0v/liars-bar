package com.example.liars_bar.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "_event")
public class Event {

    @Id
    @GeneratedValue
    private int id;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Action action = Action.THROW;

    @Builder.Default
    Instant endTime = Instant.now().plusSeconds(45);

    @OneToOne(mappedBy = "event")
    private Player player;
}
