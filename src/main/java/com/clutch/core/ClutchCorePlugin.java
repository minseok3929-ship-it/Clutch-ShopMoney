package com.clutch.core;

import com.clutch.core.command.SpawnCommand;
import com.clutch.core.command.WildCommand;
import com.clutch.core.integration.ShopMoneyBridge;
import com.clutch.core.listener.MoneyApiHookListener;
import com.clutch.core.listener.PlayerCastCancelListener;
import com.clutch.core.listener.PlayerJoinListener;
import com.clutch.core.scoreboard.PlayerScoreboardService;
import com.clutch.core.service.RTPService;
import com.clutch.core.service.TeleportCastService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ClutchCorePlugin extends JavaPlugin {
    private PlayerScoreboardService scoreboardService;
    private RTPService rtpService;
    private TeleportCastService teleportCastService;
    private ShopMoneyBridge shopMoneyBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        shopMoneyBridge = new ShopMoneyBridge(this);
        shopMoneyBridge.resolveMoneyApi();
        scoreboardService = new PlayerScoreboardService(this, shopMoneyBridge);
        rtpService = new RTPService(this);
        teleportCastService = new TeleportCastService(this);

        registerCommand("야생", new WildCommand(this, rtpService));
        registerCommand("wild", new WildCommand(this, rtpService));
        registerCommand("스폰", new SpawnCommand(this, teleportCastService));
        registerCommand("spawn", new SpawnCommand(this, teleportCastService));

        getServer().getPluginManager().registerEvents(
                new PlayerJoinListener(this, scoreboardService), this
        );
        getServer().getPluginManager().registerEvents(
                new PlayerCastCancelListener(teleportCastService), this
        );
        getServer().getPluginManager().registerEvents(
                new MoneyApiHookListener(shopMoneyBridge), this
        );

        scoreboardService.startUpdater();
    }

    @Override
    public void onDisable() {
        if (scoreboardService != null) {
            scoreboardService.shutdown();
        }
        if (teleportCastService != null) {
            teleportCastService.shutdown();
        }
    }

    public String getPrefix() {
        return getConfig().getString("prefix", "§8[CLUTCH] ");
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
        } else {
            getLogger().warning("Command not found in plugin.yml: " + name);
        }
    }
}
