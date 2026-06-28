package com.chunkmc.usertitle.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LocalTitleStorage {

    private final JavaPlugin plugin;
    private final File storageFile;
    private YamlConfiguration config;

    public LocalTitleStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "playerdata.yml");
        reload();
    }

    public void reload() {
        if (!storageFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                storageFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create playerdata.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(storageFile);
    }

    private String path(UUID uuid, String key) {
        return "players." + uuid.toString() + "." + key;
    }

    public String getActiveTitle(UUID uuid) {
        return config.getString(path(uuid, "active"), null);
    }

    public void setActiveTitle(UUID uuid, String titleId) {
        if (titleId == null || titleId.isEmpty()) {
            config.set(path(uuid, "active"), null);
        } else {
            config.set(path(uuid, "active"), titleId);
        }
        save();
    }

    public Set<String> getOwnedTitles(UUID uuid) {
        List<String> list = config.getStringList(path(uuid, "owned"));
        return new HashSet<>(list);
    }

    public void addOwnedTitle(UUID uuid, String titleId) {
        Set<String> owned = getOwnedTitles(uuid);
        owned.add(titleId);
        config.set(path(uuid, "owned"), List.copyOf(owned));
        save();
    }

    public boolean ownsTitle(UUID uuid, String titleId) {
        return getOwnedTitles(uuid).contains(titleId);
    }

    private void save() {
        try {
            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save playerdata.yml: " + e.getMessage());
        }
    }
}
