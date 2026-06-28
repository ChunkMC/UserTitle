package com.chunkmc.usertitle.listener;

import com.chunkmc.usertitle.UserTitlePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TabCompleteListener implements Listener {

    private final UserTitlePlugin plugin;

    public TabCompleteListener(UserTitlePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player title data async (with backend + local fallback)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.loadPlayerData(player.getUniqueId());

                // Update display on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        plugin.updatePlayerDisplayName(player);
                        plugin.updatePlayerTabList(player);
                    }
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load title data for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getTitleCache().remove(event.getPlayer().getUniqueId());
    }
}
