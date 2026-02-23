package com.clutch.shopmoney;

import com.clutch.shopmoney.command.*;
import com.clutch.shopmoney.gui.GuiFactory;
import com.clutch.shopmoney.listener.*;
import com.clutch.shopmoney.repository.AccountRepository;
import com.clutch.shopmoney.repository.Database;
import com.clutch.shopmoney.repository.ShopRepository;
import com.clutch.shopmoney.service.ChatInputService;
import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ClutchShopMoneyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        Database database = new Database(this);
        MessageUtil messageUtil = new MessageUtil(this);
        try {
            database.init();
        } catch (Exception e) {
            getLogger().severe("DB init failed: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        AccountRepository accountRepository = new AccountRepository(database);
        ShopRepository shopRepository = new ShopRepository(database);
        MoneyService moneyService = new MoneyService(accountRepository);
        ShopService shopService = new ShopService(this, shopRepository);
        ChatInputService chatInputService = new ChatInputService();
        GuiFactory guiFactory = new GuiFactory(this, shopService);
        try { shopService.load(); } catch (Exception ignored) {}
        shopService.restoreFromWorldScan();

        AdminCommand adminCommand = new AdminCommand(this, moneyService, shopService, guiFactory, messageUtil);
        registerCommand("출금", new WithdrawCommand(this, moneyService, messageUtil), null);
        registerCommand("돈", new MoneyCommand(moneyService, messageUtil), new MoneyTabCompleter());
        registerCommand("t", adminCommand, adminCommand);

        Bukkit.getPluginManager().registerEvents(new JoinListener(moneyService, messageUtil), this);
        Bukkit.getPluginManager().registerEvents(new PlayerInteractListener(this, moneyService, shopService, messageUtil), this);
        Bukkit.getPluginManager().registerEvents(new ShopEntityListener(shopService, guiFactory, messageUtil), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(moneyService, shopService, guiFactory, messageUtil, chatInputService), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(chatInputService), this);
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor, org.bukkit.command.TabCompleter tabCompleter) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) return;
        cmd.setExecutor(executor);
        if (tabCompleter != null) cmd.setTabCompleter(tabCompleter);
    }
}
