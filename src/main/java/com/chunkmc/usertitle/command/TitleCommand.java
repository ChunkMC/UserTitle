package com.chunkmc.usertitle.command;

import com.chunkmc.usertitle.UserTitlePlugin;
import com.chunkmc.usertitle.gui.TitleGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TitleCommand implements CommandExecutor {

    private final UserTitlePlugin plugin;
    private final TitleGui titleGui;

    public TitleCommand(UserTitlePlugin plugin) {
        this.plugin = plugin;
        this.titleGui = new TitleGui(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c这个命令只能由玩家使用！");
            return true;
        }

        // Refresh player data before opening GUI
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String uuid = player.getUniqueId().toString();

                // Fetch latest active title
                String activeTitleId = plugin.getBackendClient().getPlayerTitle(uuid);
                plugin.getTitleCache().setActiveTitle(player.getUniqueId(), activeTitleId);

                // Fetch latest owned titles
                var ownedTitles = plugin.getBackendClient().getPlayerOwnedTitles(uuid);
                for (String titleId : ownedTitles) {
                    plugin.getTitleCache().addOwnedTitle(player.getUniqueId(), titleId);
                }

                // Open GUI on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    titleGui.open(player);
                });
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to refresh title data for " + player.getName() + ": " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§c加载头衔数据失败，请稍后再试");
                });
            }
        });

        return true;
    }
}
