package com.clutch.core.integration;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

public class ShopMoneyBridge {
    private final JavaPlugin plugin;
    private Plugin shopMoneyPlugin;
    private Method directBalanceMethod;

    public ShopMoneyBridge(JavaPlugin plugin) {
        this.plugin = plugin;
        hook();
    }

    private void hook() {
        shopMoneyPlugin = plugin.getServer().getPluginManager().getPlugin("Clutch-ShopMoney");
        if (shopMoneyPlugin == null) {
            shopMoneyPlugin = plugin.getServer().getPluginManager().getPlugin("ShopMoney");
        }

        if (shopMoneyPlugin == null) {
            return;
        }

        try {
            directBalanceMethod = shopMoneyPlugin.getClass().getMethod("getBalance", UUID.class);
        } catch (NoSuchMethodException ignored) {
            directBalanceMethod = null;
        }
    }

    public Optional<String> getBalanceDisplay(UUID uuid) {
        if (shopMoneyPlugin == null || !shopMoneyPlugin.isEnabled()) {
            return Optional.empty();
        }

        if (directBalanceMethod != null) {
            try {
                Object result = directBalanceMethod.invoke(shopMoneyPlugin, uuid);
                if (result instanceof Number number) {
                    return Optional.of(String.format("%,.2f", number.doubleValue()));
                }
                if (result != null) {
                    return Optional.of(result.toString());
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                return Optional.empty();
            }
        }

        try {
            Method getter = shopMoneyPlugin.getClass().getMethod("getApi");
            Object api = getter.invoke(shopMoneyPlugin);
            if (api != null) {
                Method apiBalance = api.getClass().getMethod("getBalance", UUID.class);
                Object result = apiBalance.invoke(api, uuid);
                if (result instanceof Number number) {
                    return Optional.of(String.format("%,.2f", number.doubleValue()));
                }
                if (result != null) {
                    return Optional.of(result.toString());
                }
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}
