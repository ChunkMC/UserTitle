package com.chunkmc.usertitle.model;

public class TitleConfig {

    private final String id;
    private final String name;
    private final TitleRarity rarity;

    public TitleConfig(String id, String name, TitleRarity rarity) {
        this.id = id;
        this.name = name;
        this.rarity = rarity;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TitleRarity getRarity() {
        return rarity;
    }

    public String getDisplayName() {
        return rarity.getColorCode() + "【" + name + "】";
    }

    public String getFormattedTooltip() {
        return rarity.getColorCode() + "【" + name + "】(" + rarity.getDisplayName() + ")";
    }
}
