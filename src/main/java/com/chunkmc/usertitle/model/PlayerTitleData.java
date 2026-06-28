package com.chunkmc.usertitle.model;

import java.util.HashSet;
import java.util.Set;

public class PlayerTitleData {

    private String activeTitleId;
    private final Set<String> ownedTitleIds;

    public PlayerTitleData() {
        this.activeTitleId = null;
        this.ownedTitleIds = new HashSet<>();
    }

    public PlayerTitleData(String activeTitleId, Set<String> ownedTitleIds) {
        this.activeTitleId = activeTitleId;
        this.ownedTitleIds = new HashSet<>(ownedTitleIds);
    }

    public String getActiveTitleId() {
        return activeTitleId;
    }

    public void setActiveTitleId(String activeTitleId) {
        this.activeTitleId = activeTitleId;
    }

    public Set<String> getOwnedTitleIds() {
        return ownedTitleIds;
    }

    public boolean ownsTitle(String titleId) {
        return ownedTitleIds.contains(titleId);
    }

    public void addTitle(String titleId) {
        ownedTitleIds.add(titleId);
    }

    public boolean hasActiveTitle() {
        return activeTitleId != null && !activeTitleId.isEmpty();
    }
}
