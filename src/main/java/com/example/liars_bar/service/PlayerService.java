package com.example.liars_bar.service;

import com.example.liars_bar.model.Player;
import com.example.liars_bar.repo.PlayerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepo playerRepo;

    public boolean existsById(Long id) {
        return playerRepo.existsById(id);
    }

    public Optional<Player> findById(Long id) {
        return playerRepo.findById(id);
    }

    public void save(Player player) {
        playerRepo.save(player);
    }

    public void delete(Player player) {
        playerRepo.delete(player);
    }

    public void reset(Long id) {
        save(Player.builder().id(id).build());
    }
}
