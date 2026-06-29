package com.chunkmc.usertitle.listener;

import com.chunkmc.usertitle.UserTitlePlugin;
import com.chunkmc.usertitle.gui.TitleGui;
import com.chunkmc.usertitle.model.PlayerTitleData;
import com.chunkmc.usertitle.model.TitleConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class GuiClickListener implements Listener {

    private final UserTitlePlugin plugin;

    public GuiClickListener(UserTitlePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if this is our title GUI
        String title = event.getView().getTitle();
        if (!title.equals(TitleGui.GUI_TITLE)) return;

        event.setCancelled(true); // Prevent taking items

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        // Find which title was clicked by matching display name
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(meta.displayName());

        TitleConfig clickedTitle = null;
        for (Map.Entry<String, TitleConfig> entry : plugin.getTitleConfigs().entrySet()) {
            if (displayName.contains(entry.getValue().name())) {
                clickedTitle = entry.getValue();
                break;
            }
        }

        if (clickedTitle == null) return;

        final TitleConfig selectedTitle = clickedTitle;
        PlayerTitleData playerData = plugin.getTitleCache().getOrCreate(player.getUniqueId());
        String titleId = selectedTitle.id();
        String activeTitleId = playerData.getActiveTitleId();

        // Check if player owns this title
        if (!playerData.ownsTitle(titleId)) {
            player.sendMessage(Component.text("§c你还没有拥有这个头衔！"));
            return;
        }

        // Toggle title (uses backend + local fallback)
        if (titleId.equals(activeTitleId)) {
            // Unequip
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.setActiveTitle(player.getUniqueId(), null);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.updatePlayerDisplayName(player);
                    plugin.updatePlayerTabList(player);
                    player.sendMessage(Component.text("§a已取消装备头衔 §6【" + selectedTitle.name() + "】"));
                    new TitleGui(plugin).open(player);
                });
            });
        } else {
            // Equip
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.setActiveTitle(player.getUniqueId(), titleId);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.updatePlayerDisplayName(player);
                    plugin.updatePlayerTabList(player);
                    player.sendMessage(Component.text("§a已装备头衔 §6【" + selectedTitle.name() + "】"));
                    new TitleGui(plugin).open(player);
                });
            });
        }
    }
}
