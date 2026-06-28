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

public class UserTitlePlugin extends JavaPlugin {

    private BackendClient backendClient;
    private CallbackServer callbackServer;
    private TitleCache titleCache;
    private Map<String, TitleConfig> titleConfigs;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize cache and title configs
        titleCache = new TitleCache();
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
        getCommand("title").setExecutor(new TitleCommand(this));

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
        // Reload titles.yml from plugin resources
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

    public BackendClient getBackendClient() {
        return backendClient;
    }

    public TitleCache getTitleCache() {
        return titleCache;
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
        return config.getRarity().getColorCode() + "[" + config.getName() + "]";
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

        TextColor color = switch (config.getRarity()) {
            case LEGENDARY -> NamedTextColor.GOLD;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case HERO -> NamedTextColor.BLUE;
            case COMMON -> NamedTextColor.WHITE;
        };

        return Component.text("[" + config.getName() + "]")
                .color(color)
                .append(Component.space());
    }

    public void updatePlayerDisplayName(Player player) {
        Component titleComponent = getActiveTitleComponent(player);
        Component nameComponent = Component.text(player.getName());
        Component displayName = titleComponent.append(nameComponent);
        player.displayName(displayName);
        player.playerListName(displayName);
    }

    public void updatePlayerTabList(Player player) {
        updatePlayerDisplayName(player);
    }
}
