package com.clutch.core.scoreboard;

import com.clutch.core.ClutchCorePlugin;
import com.clutch.core.integration.ShopMoneyBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerScoreboardService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ClutchCorePlugin plugin;
    private final ShopMoneyBridge shopMoneyBridge;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    private int taskId = -1;

    public PlayerScoreboardService(ClutchCorePlugin plugin, ShopMoneyBridge shopMoneyBridge) {
        this.plugin = plugin;
        this.shopMoneyBridge = shopMoneyBridge;
    }

    public void startUpdater() {
        long periodTicks = Math.max(20L, plugin.getConfig().getLong("scoreboard.update-ticks", 20L));
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, 20L, periodTicks);
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        playerBoards.clear();
    }

    public void show(Player player) {
        if (!player.hasPermission("clutch.scoreboard")) {
            return;
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("clutch", "dummy", "§8CLUTCH");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        createLine(board, objective, "line1", "§f닉네임: §e", 8);
        createLine(board, objective, "line2", "§f내 보유금: §e", 7);
        createLine(board, objective, "line3", "§f시간: §e", 6);
        createLine(board, objective, "line4", "§f바이옴: §e", 5);

        player.setScoreboard(board);
        playerBoards.put(player.getUniqueId(), board);
        update(player);
    }

    public void hide(Player player) {
        playerBoards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (playerBoards.containsKey(player.getUniqueId())) {
                update(player);
            }
        }
    }

    private void update(Player player) {
        Scoreboard board = playerBoards.get(player.getUniqueId());
        if (board == null) {
            return;
        }

        setSuffix(board, "line1", trim(player.getName()));
        setSuffix(board, "line2", trim(shopMoneyBridge.getBalanceDisplay(player.getUniqueId())));
        setSuffix(board, "line3", trim(LocalTime.now().format(TIME_FORMATTER)));
        setSuffix(board, "line4", trim(formatBiome(player.getLocation().getBlock().getBiome())));
    }

    private String formatBiome(Biome biome) {
        String biomeName = biome.name().toLowerCase(Locale.ROOT);
        return Arrays.stream(biomeName.split("_"))
                .map(part -> part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private void createLine(Scoreboard board, Objective objective, String teamName, String label, int score) {
        Team team = board.registerNewTeam(teamName);
        String entry = ChatColor.values()[score].toString() + ChatColor.RESET;
        team.addEntry(entry);
        team.setPrefix(label);
        objective.getScore(entry).setScore(score);
    }

    private void setSuffix(Scoreboard board, String teamName, String value) {
        Team team = board.getTeam(teamName);
        if (team != null) {
            String normalized = value.length() > 64 ? value.substring(0, 64) : value;
            team.setSuffix(normalized);
        }
    }

    private String trim(String text) {
        return text.length() > 64 ? text.substring(0, 64) : text;
    }
}
