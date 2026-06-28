package com.chunkmc.usertitle.listener;

import com.chunkmc.usertitle.UserTitlePlugin;
import com.chunkmc.usertitle.model.TitleConfig;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final UserTitlePlugin plugin;

    public ChatListener(UserTitlePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String activeTitleId = plugin.getTitleCache().getActiveTitleId(player.getUniqueId());

        if (activeTitleId == null || activeTitleId.isEmpty()) {
            return;
        }

        TitleConfig config = plugin.getTitleConfig(activeTitleId);
        if (config == null) {
            return;
        }

        TextColor color = switch (config.getRarity()) {
            case LEGENDARY -> NamedTextColor.GOLD;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case HERO -> NamedTextColor.BLUE;
            case COMMON -> NamedTextColor.WHITE;
        };

        // Update player display name before chat renders
        Component titlePrefix = Component.text("[" + config.getName() + "]")
                .color(color)
                .append(Component.space());

        player.displayName(titlePrefix.append(Component.text(player.getName())));
    }
}
