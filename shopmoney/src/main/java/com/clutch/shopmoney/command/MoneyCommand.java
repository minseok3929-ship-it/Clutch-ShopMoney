package com.clutch.shopmoney.command;

import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.NumberUtil;
import com.clutch.shopmoney.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public class MoneyCommand implements CommandExecutor {
    private final MoneyService moneyService;
    private final MessageUtil messageUtil;

    public MoneyCommand(MoneyService moneyService, MessageUtil messageUtil) {
        this.moneyService = moneyService;
        this.messageUtil = messageUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!player.hasPermission("clutch.money.use")) {
            messageUtil.send(player, "§c권한이 없습니다.");
            return true;
        }

        if (args.length == 0) {
            try {
                long balance = moneyService.balance(player.getUniqueId());
                messageUtil.send(player, "§f내 보유금: §e" + NumberUtil.formatAmount(balance));
            } catch (SQLException e) {
                messageUtil.send(player, "§c잔액 조회 실패");
            }
            return true;
        }

        if (args.length != 3 || !"보내기".equals(args[0])) {
            messageUtil.send(player, "§f사용법: /돈 또는 /돈 보내기 <닉네임> <금액>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        Long amount = NumberUtil.parsePositiveLong(args[2]);
        if (target == null || !target.isOnline()) {
            messageUtil.send(player, "§c대상 플레이어가 오프라인입니다.");
            return true;
        }
        if (amount == null) {
            messageUtil.send(player, "§c올바른 금액을 입력하세요.");
            return true;
        }

        try {
            if (!moneyService.withdraw(player.getUniqueId(), amount)) {
                messageUtil.send(player, "§c금액이 부족합니다!");
                SoundUtil.error(player);
                return true;
            }
            moneyService.deposit(target.getUniqueId(), amount);
            messageUtil.send(player, "§f송금 완료: §e" + target.getName() + " " + NumberUtil.formatAmount(amount));
            messageUtil.send(target, "§f송금 수신: §e" + player.getName() + " " + NumberUtil.formatAmount(amount));
        } catch (SQLException e) {
            messageUtil.send(player, "§c송금 실패");
        }
        return true;
    }
}
