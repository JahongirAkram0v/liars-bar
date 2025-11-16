package com.example.liars_bar.service;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.repo.PlayerRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepo playerRepo;

    public Optional<Player> findById(Long id) {
        return playerRepo.findById(id);
    }

    public void save(Player player) {
        playerRepo.save(player);
    }

    public int index(Group group) {

        List<Integer> indices = group.getPlayers().stream()
                .filter(p -> p.getIsActive() && p.getIsAlive())
                .map(Player::getPlayerIndex)
                .toList();
        System.out.println(indices);

        for (int i: indices) {
            if (group.getTurn() < i) return i;
        }
        return indices.getFirst();
    }
}
