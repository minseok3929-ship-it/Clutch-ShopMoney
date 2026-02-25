package com.clutch.scoreboard.command;

import com.clutch.scoreboard.service.ScoreboardService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ClutchScoreboardCommand implements CommandExecutor, TabCompleter {
    private final ScoreboardService scoreboardService;

    public ClutchScoreboardCommand(ScoreboardService scoreboardService) {
        this.scoreboardService = scoreboardService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!player.hasPermission("clutch.scoreboard")) {
            player.sendMessage(ChatColor.RED + "권한이 없습니다.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§7사용법: /clutchsb <on|off>");
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            scoreboardService.enable(player);
            player.sendMessage("§8[CLUTCH] §f스코어보드를 켰습니다.");
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            scoreboardService.hide(player);
            player.sendMessage("§8[CLUTCH] §f스코어보드를 껐습니다.");
            return true;
        }

        player.sendMessage("§7사용법: /clutchsb <on|off>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
