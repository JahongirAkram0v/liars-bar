package com.example.liars_bar.repo;

import com.example.liars_bar.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepo extends JpaRepository<Player, Long> {
}
