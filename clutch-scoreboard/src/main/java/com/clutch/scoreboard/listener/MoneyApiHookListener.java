package com.clutch.scoreboard.listener;

import com.clutch.scoreboard.service.MoneyBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class MoneyApiHookListener implements Listener {
    private final MoneyBridge moneyBridge;

    public MoneyApiHookListener(MoneyBridge moneyBridge) {
        this.moneyBridge = moneyBridge;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("ClutchShopMoney")) {
            moneyBridge.resolve();
        }
    }
}
