package com.clutch.scoreboard.service;

import com.clutch.scoreboard.ClutchScoreboardPlugin;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;

public class ScoreboardService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final java.text.NumberFormat BALANCE_FORMATTER = java.text.NumberFormat.getInstance(Locale.US);

    private static final String LINE_NAME = ChatColor.BLACK + "" + ChatColor.WHITE;
    private static final String LINE_BALANCE = ChatColor.BLACK + "" + ChatColor.GREEN;
    private static final String LINE_TIME = ChatColor.BLACK + "" + ChatColor.AQUA;
    private static final String LINE_WORLD = ChatColor.BLACK + "" + ChatColor.YELLOW;
    private static final String LINE_BIOME = ChatColor.BLACK + "" + ChatColor.LIGHT_PURPLE;

    private final ClutchScoreboardPlugin plugin;
    private final MoneyBridge moneyBridge;
    private final Map<UUID, PlayerBoard> boards = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();
    private int taskId = -1;

    public ScoreboardService(ClutchScoreboardPlugin plugin, MoneyBridge moneyBridge) {
        this.plugin = plugin;
        this.moneyBridge = moneyBridge;
    }

    public void start() {
        int period = Math.max(1, plugin.getConfig().getInt("scoreboard.update-ticks", 20));
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::updateAll, period, period);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void show(Player player) {
        if (!player.hasPermission("clutch.scoreboard") || disabled.contains(player.getUniqueId())) {
            return;
        }

        PlayerBoard playerBoard = boards.computeIfAbsent(player.getUniqueId(), id -> createBoard());
        player.setScoreboard(playerBoard.scoreboard());
        update(player);
    }

    public void hide(Player player) {
        boards.remove(player.getUniqueId());
        disabled.add(player.getUniqueId());
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void enable(Player player) {
        disabled.remove(player.getUniqueId());
        show(player);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        disabled.remove(player.getUniqueId());
    }

    public void clearAll() {
        boards.clear();
        disabled.clear();
    }

    public void updateAll() {
        Bukkit.getOnlinePlayers().forEach(this::update);
    }

    public void update(Player player) {
        if (!player.hasPermission("clutch.scoreboard") || disabled.contains(player.getUniqueId())) {
            return;
        }

        PlayerBoard board = boards.computeIfAbsent(player.getUniqueId(), id -> createBoard());
        setSuffix(board.teamName(), trim(player.getName()));

        String balance = moneyBridge.getBalance(player.getUniqueId())
                .stream()
                .mapToObj(BALANCE_FORMATTER::format)
                .findFirst()
                .orElse("N/A");
        setSuffix(board.teamBalance(), trim(balance));

        setSuffix(board.teamTime(), LocalTime.now().format(TIME_FORMATTER));
        setSuffix(board.teamWorld(), trim(player.getWorld().getName()));
        setSuffix(board.teamBiome(), trim(formatBiome(player.getLocation().getBlock().getBiome().name())));

        if (player.getScoreboard() != board.scoreboard()) {
            player.setScoreboard(board.scoreboard());
        }
    }

    private void setSuffix(Team team, String suffix) {
        team.setSuffix(suffix);
    }

    private PlayerBoard createBoard() {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("clutch", Criteria.DUMMY, "§8CLUTCH");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        Team teamName = scoreboard.registerNewTeam("name");
        teamName.addEntry(LINE_NAME);
        teamName.setPrefix("§f닉네임: §7");
        objective.getScore(LINE_NAME).setScore(5);

        Team teamBalance = scoreboard.registerNewTeam("balance");
        teamBalance.addEntry(LINE_BALANCE);
        teamBalance.setPrefix("§f보유금: §7");
        objective.getScore(LINE_BALANCE).setScore(4);

        Team teamTime = scoreboard.registerNewTeam("time");
        teamTime.addEntry(LINE_TIME);
        teamTime.setPrefix("§f시간: §7");
        objective.getScore(LINE_TIME).setScore(3);

        Team teamWorld = scoreboard.registerNewTeam("world");
        teamWorld.addEntry(LINE_WORLD);
        teamWorld.setPrefix("§f월드: §7");
        objective.getScore(LINE_WORLD).setScore(2);

        Team teamBiome = scoreboard.registerNewTeam("biome");
        teamBiome.addEntry(LINE_BIOME);
        teamBiome.setPrefix("§f바이옴: §7");
        objective.getScore(LINE_BIOME).setScore(1);

        return new PlayerBoard(scoreboard, teamName, teamBalance, teamTime, teamWorld, teamBiome);
    }

    private String formatBiome(String biomeName) {
        String[] tokens = biomeName.toLowerCase(Locale.ROOT).split("_");
        StringJoiner joiner = new StringJoiner(" ");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            joiner.add(Character.toUpperCase(token.charAt(0)) + token.substring(1));
        }
        return joiner.toString();
    }

    private String trim(String value) {
        return value.length() > 64 ? value.substring(0, 64) : value;
    }

    private record PlayerBoard(Scoreboard scoreboard, Team teamName, Team teamBalance, Team teamTime, Team teamWorld,
                               Team teamBiome) {
    }
}
