package com.clutch.scoreboard.listener;

import com.clutch.scoreboard.service.ScoreboardService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {
    private final ScoreboardService scoreboardService;

    public QuitListener(ScoreboardService scoreboardService) {
        this.scoreboardService = scoreboardService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboardService.remove(event.getPlayer());
    }
}
