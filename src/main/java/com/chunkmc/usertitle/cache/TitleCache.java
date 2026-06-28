package com.chunkmc.usertitle.cache;

import com.chunkmc.usertitle.model.PlayerTitleData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TitleCache {

    private final ConcurrentHashMap<UUID, PlayerTitleData> cache = new ConcurrentHashMap<>();

    public PlayerTitleData get(UUID uuid) {
        return cache.get(uuid);
    }

    public void put(UUID uuid, PlayerTitleData data) {
        cache.put(uuid, data);
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public PlayerTitleData getOrCreate(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new PlayerTitleData());
    }

    public void setActiveTitle(UUID uuid, String titleId) {
        PlayerTitleData data = getOrCreate(uuid);
        data.setActiveTitleId(titleId);
    }

    public void addOwnedTitle(UUID uuid, String titleId) {
        PlayerTitleData data = getOrCreate(uuid);
        data.addTitle(titleId);
    }

    public String getActiveTitleId(UUID uuid) {
        PlayerTitleData data = cache.get(uuid);
        return data != null ? data.getActiveTitleId() : null;
    }

    public boolean ownsTitle(UUID uuid, String titleId) {
        PlayerTitleData data = cache.get(uuid);
        return data != null && data.ownsTitle(titleId);
    }
}
