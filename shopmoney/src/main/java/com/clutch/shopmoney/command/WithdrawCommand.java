package com.clutch.shopmoney.command;

import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.util.ItemUtil;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.NumberUtil;
import com.clutch.shopmoney.util.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class WithdrawCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final MoneyService moneyService;
    private final MessageUtil messageUtil;

    public WithdrawCommand(JavaPlugin plugin, MoneyService moneyService, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.moneyService = moneyService;
        this.messageUtil = messageUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("clutch.money.use")) { messageUtil.send(player, "§c권한이 없습니다."); return true; }
        if (args.length != 1) { messageUtil.send(player, "§f사용법: /출금 <금액>"); return true; }
        Long amount = NumberUtil.parsePositiveLong(args[0]);
        if (amount == null) { messageUtil.send(player, "§c올바른 금액을 입력하세요."); return true; }
        if (player.getInventory().firstEmpty() == -1) {
            messageUtil.send(player, "§c빈 공간이 없습니다!");
            SoundUtil.error(player);
            return true;
        }
        try {
            if (!moneyService.withdraw(player.getUniqueId(), amount)) {
                messageUtil.send(player, "§c금액이 부족합니다!");
                SoundUtil.error(player);
                return true;
            }
            int cmd = plugin.getConfig().getInt("cash.customModelData", 10001);
            ItemStack cash = ItemUtil.createCash(plugin, amount, cmd);
            player.getInventory().addItem(cash);
            messageUtil.send(player, "§f출금 완료: §e" + amount);
        } catch (SQLException e) {
            messageUtil.send(player, "§c출금 처리 실패");
        }
        return true;
    }
}
