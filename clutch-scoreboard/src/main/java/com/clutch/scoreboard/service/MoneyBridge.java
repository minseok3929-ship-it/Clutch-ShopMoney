package com.clutch.scoreboard.service;

import com.clutch.scoreboard.ClutchScoreboardPlugin;
import com.clutch.shopmoney.api.MoneyAPI;
import org.bukkit.Bukkit;

import java.util.OptionalLong;
import java.util.UUID;

public class MoneyBridge {
    private final ClutchScoreboardPlugin plugin;
    private volatile MoneyAPI moneyAPI;

    public MoneyBridge(ClutchScoreboardPlugin plugin) {
        this.plugin = plugin;
    }

    public void resolve() {
        this.moneyAPI = Bukkit.getServicesManager().load(MoneyAPI.class);
        if (this.moneyAPI == null) {
            plugin.getLogger().info("MoneyAPI not found. Balance will be shown as N/A.");
        } else {
            plugin.getLogger().info("MoneyAPI hooked successfully.");
        }
    }

    public OptionalLong getBalance(UUID playerId) {
        MoneyAPI api = this.moneyAPI;
        if (api == null) {
            return OptionalLong.empty();
        }

        try {
            return OptionalLong.of(api.getBalance(playerId));
        } catch (Exception ex) {
            return OptionalLong.empty();
        }
    }
}
