package com.chunkmc.usertitle;

import com.chunkmc.usertitle.cache.TitleCache;
import com.chunkmc.usertitle.command.TitleCommand;
import com.chunkmc.usertitle.grpc.BackendClient;
import com.chunkmc.usertitle.grpc.CallbackServer;
import com.chunkmc.usertitle.listener.ChatListener;
import com.chunkmc.usertitle.listener.GuiClickListener;
import com.chunkmc.usertitle.listener.TabCompleteListener;
import com.chunkmc.usertitle.model.TitleConfig;
import com.chunkmc.usertitle.model.TitleRarity;
import com.chunkmc.usertitle.storage.LocalTitleStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UserTitlePlugin extends JavaPlugin {

    private BackendClient backendClient;
    private CallbackServer callbackServer;
    private TitleCache titleCache;
    private LocalTitleStorage localStorage;
    private Map<String, TitleConfig> titleConfigs;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize cache and title configs
        titleCache = new TitleCache();
        localStorage = new LocalTitleStorage(this);
        titleConfigs = new HashMap<>();
        loadTitleConfigs();

        // Initialize gRPC backend client
        String backendHost = getConfig().getString("backend.host", "localhost");
        int backendPort = getConfig().getInt("backend.port", 8080);
        backendClient = new BackendClient(backendHost, backendPort, this);

        // Start gRPC callback server
        int serverPort = getConfig().getInt("server.port", 8082);
        try {
            callbackServer = new CallbackServer(serverPort, this);
            callbackServer.start();
        } catch (IOException e) {
            getLogger().severe("Failed to start gRPC callback server on port " + serverPort + ": " + e.getMessage());
        }

        // Register commands
        getCommand("ut").setExecutor(new TitleCommand(this));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TabCompleteListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiClickListener(this), this);

        getLogger().info("ChunkMC UserTitle plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (callbackServer != null) {
            callbackServer.stop();
        }
        if (backendClient != null) {
            backendClient.shutdown();
        }
        getLogger().info("ChunkMC UserTitle plugin disabled!");
    }

    private void loadTitleConfigs() {
        titleConfigs.clear();
        saveResource("titles.yml", false);
        org.bukkit.configuration.file.YamlConfiguration titlesConfig =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.File(getDataFolder(), "titles.yml"));

        ConfigurationSection titlesSection = titlesConfig.getConfigurationSection("titles");
        if (titlesSection == null) {
            getLogger().warning("No titles section found in titles.yml");
            return;
        }

        for (String id : titlesSection.getKeys(false)) {
            ConfigurationSection titleSection = titlesSection.getConfigurationSection(id);
            if (titleSection == null) continue;

            String name = titleSection.getString("name", id);
            String rarityStr = titleSection.getString("rarity", "COMMON");
            TitleRarity rarity = TitleRarity.fromString(rarityStr);

            titleConfigs.put(id, new TitleConfig(id, name, rarity));
        }

        getLogger().info("Loaded " + titleConfigs.size() + " title configurations");
    }

    /**
     * Load player title data from backend with local storage fallback.
     */
    public void loadPlayerData(UUID uuid) {
        String uuidStr = uuid.toString();

        // Try backend first
        String activeTitleId = backendClient.getPlayerTitle(uuidStr);
        java.util.List<String> ownedTitles = backendClient.getPlayerOwnedTitles(uuidStr);

        if (activeTitleId != null || !ownedTitles.isEmpty()) {
            // Backend available - use backend data and sync to local
            titleCache.setActiveTitle(uuid, activeTitleId);
            for (String titleId : ownedTitles) {
                titleCache.addOwnedTitle(uuid, titleId);
            }
            // Sync to local storage
            localStorage.setActiveTitle(uuid, activeTitleId);
            for (String titleId : ownedTitles) {
                localStorage.addOwnedTitle(uuid, titleId);
            }
            getLogger().info("Loaded title data from backend for " + uuidStr);
        } else {
            // Backend unavailable - use local storage
            activeTitleId = localStorage.getActiveTitle(uuid);
            Set<String> localOwned = localStorage.getOwnedTitles(uuid);
            titleCache.setActiveTitle(uuid, activeTitleId);
            for (String titleId : localOwned) {
                titleCache.addOwnedTitle(uuid, titleId);
            }
            getLogger().info("Loaded title data from local storage for " + uuidStr);
        }
    }

    /**
     * Set player's active title with backend sync and local fallback.
     */
    public boolean setActiveTitle(UUID uuid, String titleId) {
        String uuidStr = uuid.toString();

        // Try backend
        boolean backendSuccess = backendClient.setPlayerTitle(uuidStr, titleId);

        // Always update local storage
        localStorage.setActiveTitle(uuid, titleId);
        titleCache.setActiveTitle(uuid, titleId);

        if (!backendSuccess) {
            getLogger().info("Backend unavailable, title saved locally for " + uuidStr);
        }

        return true;
    }

    /**
     * Add a title to player's collection with backend sync and local fallback.
     */
    public boolean addPlayerTitle(UUID uuid, String titleId) {
        String uuidStr = uuid.toString();

        // Try backend
        boolean backendSuccess = backendClient.addPlayerTitle(uuidStr, titleId);

        // Always update local storage
        localStorage.addOwnedTitle(uuid, titleId);
        titleCache.addOwnedTitle(uuid, titleId);

        if (!backendSuccess) {
            getLogger().info("Backend unavailable, title added locally for " + uuidStr);
        }

        return true;
    }

    public BackendClient getBackendClient() {
        return backendClient;
    }

    public TitleCache getTitleCache() {
        return titleCache;
    }

    public LocalTitleStorage getLocalStorage() {
        return localStorage;
    }

    public Map<String, TitleConfig> getTitleConfigs() {
        return titleConfigs;
    }

    public TitleConfig getTitleConfig(String titleId) {
        return titleConfigs.get(titleId);
    }

    public String getActiveTitlePrefix(Player player) {
        String activeTitleId = titleCache.getActiveTitleId(player.getUniqueId());
        if (activeTitleId == null || activeTitleId.isEmpty()) {
            return null;
        }
        TitleConfig config = titleConfigs.get(activeTitleId);
        if (config == null) {
            return null;
        }
        return config.rarity().getColorCode() + "[" + config.name() + "]";
    }

    public Component getActiveTitleComponent(Player player) {
        String activeTitleId = titleCache.getActiveTitleId(player.getUniqueId());
        if (activeTitleId == null || activeTitleId.isEmpty()) {
            return Component.empty();
        }
        TitleConfig config = titleConfigs.get(activeTitleId);
        if (config == null) {
            return Component.empty();
        }

        TextColor color = switch (config.rarity()) {
            case LEGENDARY -> NamedTextColor.GOLD;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case HERO -> NamedTextColor.BLUE;
            case COMMON -> NamedTextColor.WHITE;
        };

        return Component.text("[" + config.name() + "]")
                .color(color)
                .append(Component.space());
    }

    public void updatePlayerDisplayName(Player player) {
        Component titleComponent = getActiveTitleComponent(player);
        Component nameComponent = Component.text(player.getName()).color(NamedTextColor.WHITE);
        Component displayName = titleComponent.append(nameComponent);
        player.displayName(displayName);
        player.playerListName(displayName);
    }

    public void updatePlayerTabList(Player player) {
        updatePlayerDisplayName(player);
    }
}
