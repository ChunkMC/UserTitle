package com.chunkmc.usertitle.gui;

import com.chunkmc.usertitle.UserTitlePlugin;
import com.chunkmc.usertitle.model.PlayerTitleData;
import com.chunkmc.usertitle.model.TitleConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TitleGui {

    public static final String GUI_TITLE = "§6头衔管理";
    private static final int GUI_SIZE = 54; // 6 rows

    private final UserTitlePlugin plugin;

    public TitleGui(UserTitlePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE);

        PlayerTitleData playerData = plugin.getTitleCache().getOrCreate(player.getUniqueId());
        Map<String, TitleConfig> allTitles = plugin.getTitleConfigs();
        String activeTitleId = playerData.getActiveTitleId();

        int slot = 0;
        for (Map.Entry<String, TitleConfig> entry : allTitles.entrySet()) {
            if (slot >= GUI_SIZE - 9) break; // Leave last row empty

            String titleId = entry.getKey();
            TitleConfig config = entry.getValue();
            boolean isOwned = playerData.ownsTitle(titleId);
            boolean isActive = titleId.equals(activeTitleId);

            ItemStack item = createTitleItem(config, isOwned, isActive);
            gui.setItem(slot, item);
            slot++;
        }

        // Fill remaining slots with glass panes
        ItemStack glassPane = createGlassPane();
        for (int i = slot; i < GUI_SIZE - 9; i++) {
            gui.setItem(i, glassPane);
        }

        player.openInventory(gui);
    }

    private ItemStack createTitleItem(TitleConfig config, boolean isOwned, boolean isActive) {
        Material material;
        if (isActive) {
            material = Material.PAPER; // Equipped = paper
        } else if (isOwned) {
            material = Material.BOOK; // Unequipped but owned = book
        } else {
            material = Material.BOOK; // Not owned = book (will be grayed out)
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        // Title name with rarity color
        TextColor color = switch (config.rarity()) {
            case LEGENDARY -> NamedTextColor.GOLD;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case HERO -> NamedTextColor.BLUE;
            case COMMON -> NamedTextColor.WHITE;
        };

        meta.displayName(Component.text("【" + config.name() + "】")
                .color(color)
                .decorate(TextDecoration.BOLD));

        // Lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("(" + config.rarity().getDisplayName() + ")")
                .color(color));
        lore.add(Component.empty());

        if (isActive) {
            lore.add(Component.text("状态：§a已装备").color(NamedTextColor.GRAY));
            lore.add(Component.text("§7点击取消装备"));
        } else if (isOwned) {
            lore.add(Component.text("状态：§c未装备").color(NamedTextColor.GRAY));
            lore.add(Component.text("§7点击装备"));
        } else {
            lore.add(Component.text("状态：§8未拥有").color(NamedTextColor.GRAY));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.text(" "));
        glass.setItemMeta(meta);
        return glass;
    }
}
