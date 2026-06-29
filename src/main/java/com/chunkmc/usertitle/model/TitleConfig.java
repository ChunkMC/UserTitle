package com.chunkmc.usertitle.model;

public record TitleConfig(
        String id,
        String name,
        TitleRarity rarity
) {
    public String getDisplayName() {
        return rarity.getColorCode() + "【" + name + "】";
    }

    public String getFormattedTooltip() {
        return rarity.getColorCode() + "【" + name + "】(" + rarity.getDisplayName() + ")";
    }
}
