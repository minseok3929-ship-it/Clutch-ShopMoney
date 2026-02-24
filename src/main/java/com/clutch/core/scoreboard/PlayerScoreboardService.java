package com.clutch.core.scoreboard;

import com.clutch.core.ClutchCorePlugin;
import com.clutch.core.integration.ShopMoneyBridge;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

        createLine(board, objective, "line1", ChatColor.GRAY + "닉네임: " + ChatColor.YELLOW, 8);
        createLine(board, objective, "line2", ChatColor.GRAY + "내 보유금: " + ChatColor.YELLOW, 7);
        createLine(board, objective, "line3", ChatColor.GRAY + "현실 시간: " + ChatColor.YELLOW, 6);
        createLine(board, objective, "line4", ChatColor.GRAY + "바이옴: " + ChatColor.YELLOW, 5);

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

        setPrefix(board, "line1", trim("§e" + player.getName()));

        Optional<String> balanceOpt = shopMoneyBridge.getBalanceDisplay(player.getUniqueId());
        setPrefix(board, "line2", trim("§e" + balanceOpt.orElse("N/A")));

        setPrefix(board, "line3", trim("§e" + LocalTime.now().format(TIME_FORMATTER)));
        setPrefix(board, "line4", trim("§e" + BiomeKoreanMapper.toKorean(player.getLocation().getBlock().getBiome())));
    }

    private void createLine(Scoreboard board, Objective objective, String teamName, String label, int score) {
        Team team = board.registerNewTeam(teamName);
        String entry = ChatColor.values()[score].toString() + ChatColor.RESET;
        team.addEntry(entry);
        team.setPrefix(label);
        objective.getScore(entry).setScore(score);
    }

    private void setPrefix(Scoreboard board, String teamName, String value) {
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
