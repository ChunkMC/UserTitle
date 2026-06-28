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

        // Load player title data from backend async
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String uuid = player.getUniqueId().toString();

                // Fetch active title
                String activeTitleId = plugin.getBackendClient().getPlayerTitle(uuid);
                plugin.getTitleCache().setActiveTitle(player.getUniqueId(), activeTitleId);

                // Fetch owned titles
                var ownedTitles = plugin.getBackendClient().getPlayerOwnedTitles(uuid);
                for (String titleId : ownedTitles) {
                    plugin.getTitleCache().addOwnedTitle(player.getUniqueId(), titleId);
                }

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
        // Clean up cache
        plugin.getTitleCache().remove(event.getPlayer().getUniqueId());
    }
}
