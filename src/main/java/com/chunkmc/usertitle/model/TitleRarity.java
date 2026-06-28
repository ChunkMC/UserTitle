package com.chunkmc.usertitle.model;

import org.bukkit.ChatColor;

public enum TitleRarity {

    LEGENDARY("传说", ChatColor.GOLD, "§6"),
    EPIC("史诗", ChatColor.DARK_PURPLE, "§5"),
    HERO("勇者", ChatColor.BLUE, "§9"),
    COMMON("普通", ChatColor.WHITE, "§f");

    private final String displayName;
    private final ChatColor chatColor;
    private final String colorCode;

    TitleRarity(String displayName, ChatColor chatColor, String colorCode) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public String getColorCode() {
        return colorCode;
    }

    public static TitleRarity fromString(String str) {
        if (str == null) return COMMON;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
