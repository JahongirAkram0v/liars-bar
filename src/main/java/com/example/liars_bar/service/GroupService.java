package com.example.liars_bar.service;

import com.example.liars_bar.model.Group;
import com.example.liars_bar.repo.GroupRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepo groupRepo;

    public boolean existsById(String substring) {
        return groupRepo.existsById(substring);
    }

    public Optional<Group> findById(String substring) {
        return groupRepo.findById(substring);
    }

    public void save(Group group) {
        groupRepo.save(group);
    }
}
