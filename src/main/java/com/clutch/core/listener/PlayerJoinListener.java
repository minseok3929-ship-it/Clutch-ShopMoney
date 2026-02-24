package com.clutch.core.listener;

import com.clutch.core.ClutchCorePlugin;
import com.clutch.core.scoreboard.PlayerScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {
    private final ClutchCorePlugin plugin;
    private final PlayerScoreboardService scoreboardService;

    public PlayerJoinListener(ClutchCorePlugin plugin, PlayerScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(plugin.getPrefix() + "§f[Clutch plugin] 활성화");
        scoreboardService.show(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboardService.hide(event.getPlayer());
    }
}
