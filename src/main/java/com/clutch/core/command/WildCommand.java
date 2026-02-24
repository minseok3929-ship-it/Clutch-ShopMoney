package com.clutch.core.command;

import com.clutch.core.ClutchCorePlugin;
import com.clutch.core.service.RTPService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WildCommand implements CommandExecutor {
    private final ClutchCorePlugin plugin;
    private final RTPService rtpService;

    public WildCommand(ClutchCorePlugin plugin, RTPService rtpService) {
        this.plugin = plugin;
        this.rtpService = rtpService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getPrefix() + "§c플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.hasPermission("clutch.move.wild")) {
            player.sendMessage(plugin.getPrefix() + "§c권한이 없습니다.");
            return true;
        }

        if (rtpService.isOnCooldown(player)) {
            long ms = rtpService.getRemainingMillis(player);
            long sec = ms / 1000;
            long min = sec / 60;
            long remSec = sec % 60;
            player.sendMessage(plugin.getPrefix() + String.format("§c쿨타임입니다! 남은 시간 : %02d분 %02d초.", min, remSec));
            return true;
        }

        Location safe = rtpService.findSafeLocation();
        if (safe == null) {
            player.sendMessage(plugin.getPrefix() + "§c안전한 위치를 찾지 못했습니다. 잠시 후 다시 시도해주세요.");
            return true;
        }

        player.teleport(safe);
        rtpService.applyCooldown(player);
        player.sendMessage(plugin.getPrefix() + "§f야생으로 이동했습니다!");
        return true;
    }
}
