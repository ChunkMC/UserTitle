package com.chunkmc.usertitle.command;

import com.chunkmc.usertitle.UserTitlePlugin;
import com.chunkmc.usertitle.gui.TitleGui;
import com.chunkmc.usertitle.model.TitleConfig;
import org.bukkit.Bukkit;
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
        // /ut - open GUI (player only)
        // /ut give <player> <titleId> - give title to player (admin)
        // /ut list - list all available titles

        if (args.length == 0) {
            // Open GUI
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§c这个命令只能由玩家使用！");
                return true;
            }

            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.loadPlayerData(player.getUniqueId());
                    plugin.getServer().getScheduler().runTask(plugin, () -> titleGui.open(player));
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load title data for " + player.getName() + ": " + e.getMessage());
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.sendMessage("§c加载头衔数据失败，请稍后再试"));
                }
            });
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> {
                // /ut give <player> <titleId>
                if (!sender.hasPermission("chunkmc.title.give")) {
                    sender.sendMessage("§c你没有权限执行此命令！");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("§c用法: /ut give <玩家名> <头衔ID>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    sender.sendMessage("§c玩家 " + args[1] + " 不在线！");
                    return true;
                }

                String titleId = args[2];
                TitleConfig config = plugin.getTitleConfig(titleId);
                if (config == null) {
                    sender.sendMessage("§c头衔ID不存在: " + titleId);
                    sender.sendMessage("§7可用头衔: " + String.join(", ", plugin.getTitleConfigs().keySet()));
                    return true;
                }

                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.addPlayerTitle(target.getUniqueId(), titleId);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a已将头衔 §6【" + config.getName() + "】§a 赠送给 " + target.getName());
                        target.sendMessage("§a你获得了新头衔 §6【" + config.getName() + "】§a！输入 /ut 查看");
                    });
                });
            }
            case "list" -> {
                sender.sendMessage("§6===== 可用头衔列表 =====");
                for (TitleConfig config : plugin.getTitleConfigs().values()) {
                    sender.sendMessage(config.getFormattedTooltip() + " §7(ID: " + config.getId() + ")");
                }
            }
            default -> {
                sender.sendMessage("§c未知子命令。用法:");
                sender.sendMessage("§7/ut §f- 打开头衔界面");
                sender.sendMessage("§7/ut give <玩家> <头衔ID> §f- 赠送头衔");
                sender.sendMessage("§7/ut list §f- 列出所有头衔");
            }
        }

        return true;
    }
}
