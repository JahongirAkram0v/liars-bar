package com.example.liars_bar.repo;

import com.example.liars_bar.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PlayerRepo extends JpaRepository<Player, Long> {
}
