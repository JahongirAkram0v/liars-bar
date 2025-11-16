package com.example.liars_bar.repo;

import com.example.liars_bar.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepo extends JpaRepository<Group, String> {
}
