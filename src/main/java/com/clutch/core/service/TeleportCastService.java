package com.clutch.core.service;

import com.clutch.core.ClutchCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportCastService {
    private final ClutchCorePlugin plugin;
    private final Map<UUID, CastingState> castingStates = new ConcurrentHashMap<>();

    public TeleportCastService(ClutchCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean startSpawnCast(Player player) {
        if (castingStates.containsKey(player.getUniqueId())) {
            return false;
        }

        String worldName = plugin.getConfig().getString("worlds.spawn", "home");
        World homeWorld = Bukkit.getWorld(worldName);
        if (homeWorld == null) {
            player.sendMessage(plugin.getPrefix() + "§c스폰 월드를 찾을 수 없습니다.");
            return false;
        }

        Location start = player.getLocation().clone();
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            CastingState state = castingStates.remove(player.getUniqueId());
            if (state == null) {
                return;
            }

            player.teleport(homeWorld.getSpawnLocation());
            player.sendMessage(plugin.getPrefix() + "§f스폰으로 이동했습니다!");
        }, 60L);

        castingStates.put(player.getUniqueId(), new CastingState(start, taskId));
        player.sendMessage(plugin.getPrefix() + "§f3초 후 스폰으로 이동합니다. 이동하거나 피격 시 취소됩니다.");
        return true;
    }

    public void cancelIfCasting(Player player) {
        CastingState state = castingStates.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        Bukkit.getScheduler().cancelTask(state.taskId());
        player.sendMessage(plugin.getPrefix() + "§c이동이 취소되었습니다.");
    }

    public boolean hasCasting(Player player) {
        return castingStates.containsKey(player.getUniqueId());
    }

    public Location getStartLocation(Player player) {
        CastingState state = castingStates.get(player.getUniqueId());
        return state == null ? null : state.startLocation();
    }

    public void shutdown() {
        for (CastingState state : castingStates.values()) {
            Bukkit.getScheduler().cancelTask(state.taskId());
        }
        castingStates.clear();
    }

    private record CastingState(Location startLocation, int taskId) {
    }
}
