package com.example.liars_bar.player;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class JdbcPlayerStore implements PlayerStore {

    private final JdbcTemplate jdbc;

    @Override
    public void upsert(long id, String name) {
        jdbc.update("""
                INSERT INTO player (id, name) VALUES (?, ?)
                ON CONFLICT(id) DO UPDATE SET name = excluded.name, updated_at = CURRENT_TIMESTAMP
                WHERE player.name <> excluded.name
                """, id, name);
    }

    @Override
    @Transactional
    public void recordGame(Collection<Long> playerIds, long winnerId) {
        for (Long id : playerIds) {
            jdbc.update("UPDATE player SET games_played = games_played + 1 WHERE id = ?", id);
        }
        jdbc.update("UPDATE player SET wins = wins + 1 WHERE id = ?", winnerId);
    }

    public int wins(long id) {
        Integer wins = jdbc.queryForObject("SELECT wins FROM player WHERE id = ?", Integer.class, id);
        return wins == null ? 0 : wins;
    }
}
