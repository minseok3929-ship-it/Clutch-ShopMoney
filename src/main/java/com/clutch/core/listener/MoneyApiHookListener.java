package com.clutch.core.listener;

import com.clutch.core.integration.ShopMoneyBridge;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;

public class MoneyApiHookListener implements Listener {
    private final ShopMoneyBridge shopMoneyBridge;

    public MoneyApiHookListener(ShopMoneyBridge shopMoneyBridge) {
        this.shopMoneyBridge = shopMoneyBridge;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if ("ClutchShopMoney".equalsIgnoreCase(event.getPlugin().getName())) {
            shopMoneyBridge.resolveMoneyApi();
        }
    }
}
