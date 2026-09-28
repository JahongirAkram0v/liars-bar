package com.example.liars_bar.player;

import java.util.Collection;

public interface PlayerStore {

    void upsert(long id, String name);

    void recordGame(Collection<Long> playerIds, long winnerId);
}
