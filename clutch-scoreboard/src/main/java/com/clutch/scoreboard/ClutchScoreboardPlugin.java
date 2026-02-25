package com.clutch.scoreboard;

import com.clutch.scoreboard.command.ClutchScoreboardCommand;
import com.clutch.scoreboard.listener.JoinListener;
import com.clutch.scoreboard.listener.MoneyApiHookListener;
import com.clutch.scoreboard.listener.QuitListener;
import com.clutch.scoreboard.service.MoneyBridge;
import com.clutch.scoreboard.service.ScoreboardService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ClutchScoreboardPlugin extends JavaPlugin {
    private MoneyBridge moneyBridge;
    private ScoreboardService scoreboardService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.moneyBridge = new MoneyBridge(this);
        this.moneyBridge.resolve();

        this.scoreboardService = new ScoreboardService(this, moneyBridge);
        this.scoreboardService.start();

        registerCommand();

        Bukkit.getPluginManager().registerEvents(new JoinListener(this, scoreboardService), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(scoreboardService), this);
        Bukkit.getPluginManager().registerEvents(new MoneyApiHookListener(moneyBridge), this);

        Bukkit.getOnlinePlayers().forEach(scoreboardService::show);
    }

    @Override
    public void onDisable() {
        if (scoreboardService != null) {
            scoreboardService.stop();
            scoreboardService.clearAll();
        }
    }

    public String getPrefix() {
        return getConfig().getString("prefix", "§8[CLUTCH] ");
    }

    private void registerCommand() {
        PluginCommand command = getCommand("clutchsb");
        if (command == null) {
            getLogger().warning("Command 'clutchsb' not found in plugin.yml");
            return;
        }

        ClutchScoreboardCommand handler = new ClutchScoreboardCommand(scoreboardService);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }
}
