package com.example.liars_bar.service;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.model.Player;
import com.example.liars_bar.repo.GroupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepo groupRepo;

    public Optional<Group> findById(String substring) {
        return groupRepo.findById(substring);
    }

    public void save(Group group) {
        groupRepo.save(group);
    }

    public void delete(Group group) {
        groupRepo.delete(group);
    }

    public void updateTurn(Group group) {

        List<Long> indices = group.getPlayers().values().stream()
                .filter(Player::isActive)
                .filter(Player::isAlive)
                .map(Player::getId)
                .sorted()
                .toList();

        if (indices.isEmpty()) {
            System.err.println("update Turn is empty");
            System.err.println("--------------------");
            return;
        }

        Long current = group.getTurn();
        for (Long id : indices) {
            if (current < id) {
                group.setTurn(id);
                save(group);
                return;
            }
        }

        group.setTurn(indices.getFirst());
        save(group);
    }

}
