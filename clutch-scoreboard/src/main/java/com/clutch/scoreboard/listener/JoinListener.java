package com.clutch.scoreboard.listener;

import com.clutch.scoreboard.ClutchScoreboardPlugin;
import com.clutch.scoreboard.service.ScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {
    private final ClutchScoreboardPlugin plugin;
    private final ScoreboardService scoreboardService;

    public JoinListener(ClutchScoreboardPlugin plugin, ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(plugin.getPrefix() + "§f[Clutch plugin] 활성화");
        scoreboardService.show(event.getPlayer());
    }
}
