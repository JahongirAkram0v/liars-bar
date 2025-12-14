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

        List<Integer> indices = group.getPlayers().values().stream()
                .filter(Player::isActive)
                .filter(Player::isAlive)
                .map(Player::getIndex)
                .sorted()
                .toList();

        int current = group.getTurn();
        for (int i = current+1; i<group.getPC(); i++) {
            if (indices.contains(i)) {
                group.setTurn(i);
                save(group);
                return;
            }
        }

        group.setTurn(indices.getFirst());
        save(group);
    }

}
