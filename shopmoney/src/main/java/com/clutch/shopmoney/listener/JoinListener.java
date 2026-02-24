package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;

public class JoinListener implements Listener {
    private final MoneyService moneyService;
    private final MessageUtil messageUtil;

    public JoinListener(MoneyService moneyService, MessageUtil messageUtil) {
        this.moneyService = moneyService;
        this.messageUtil = messageUtil;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            moneyService.ensure(event.getPlayer().getUniqueId(), event.getPlayer().getName());
        } catch (SQLException e) {
            messageUtil.send(event.getPlayer(), "§c계좌 초기화 실패");
            return;
        }
        if (event.getPlayer() != null) {
            event.getPlayer().sendMessage(messageUtil.format("§f[Clutch plugin] 활성화"));
        }
    }
}
