package com.clutch.core.listener;

import com.clutch.core.service.TeleportCastService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerCastCancelListener implements Listener {
    private final TeleportCastService teleportCastService;

    public PlayerCastCancelListener(TeleportCastService teleportCastService) {
        this.teleportCastService = teleportCastService;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!teleportCastService.hasCasting(player)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            teleportCastService.cancelIfCasting(player);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            teleportCastService.cancelIfCasting(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        teleportCastService.cancelIfCasting(event.getPlayer());
    }
}
