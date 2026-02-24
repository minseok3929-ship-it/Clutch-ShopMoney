package com.clutch.core.command;

import com.clutch.core.ClutchCorePlugin;
import com.clutch.core.service.TeleportCastService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {
    private final ClutchCorePlugin plugin;
    private final TeleportCastService teleportCastService;

    public SpawnCommand(ClutchCorePlugin plugin, TeleportCastService teleportCastService) {
        this.plugin = plugin;
        this.teleportCastService = teleportCastService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.hasPermission("clutch.move.spawn")) {
            player.sendMessage(plugin.getPrefix() + "§c권한이 없습니다.");
            return true;
        }

        teleportCastService.startSpawnCast(player);
        return true;
    }
}
