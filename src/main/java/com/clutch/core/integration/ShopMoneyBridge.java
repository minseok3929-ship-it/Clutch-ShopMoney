package com.clutch.core.integration;

import com.clutch.shopmoney.api.MoneyAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class ShopMoneyBridge {
    private final JavaPlugin plugin;
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
    private MoneyAPI moneyAPI;
    private boolean missingLogged;

    public ShopMoneyBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void resolveMoneyApi() {
        RegisteredServiceProvider<MoneyAPI> registration = Bukkit.getServicesManager().getRegistration(MoneyAPI.class);
        if (registration == null) {
            moneyAPI = null;
            if (!missingLogged) {
                plugin.getLogger().warning("[Clutch] MoneyAPI not found. ShopMoney plugin missing or not registered.");
                missingLogged = true;
            }
            return;
        }

        moneyAPI = registration.getProvider();
        missingLogged = false;
    }

    public String getBalanceDisplay(UUID uuid) {
        MoneyAPI api = this.moneyAPI;
        if (api == null) {
            return "N/A";
        }

        long balance = api.getBalance(uuid);
        if (balance < 0L) {
            return "N/A";
        }
        return numberFormat.format(balance);
    }
}
