package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.ItemUtil;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class PlayerInteractListener implements Listener {
    private final JavaPlugin plugin;
    private final MoneyService moneyService;
    private final ShopService shopService;
    private final MessageUtil messageUtil;

    public PlayerInteractListener(JavaPlugin plugin, MoneyService moneyService, ShopService shopService, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.moneyService = moneyService;
        this.shopService = shopService;
        this.messageUtil = messageUtil;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        Long amount = ItemUtil.getCashAmount(plugin, item);
        if (amount != null) {
            event.setCancelled(true);
            try {
                moneyService.deposit(player.getUniqueId(), amount);
                if (item != null) item.setAmount(item.getAmount() - 1);
                messageUtil.send(player, "§f입금 완료: §e" + amount);
                SoundUtil.success(player);
            } catch (SQLException e) {
                messageUtil.send(player, "§c입금 실패");
                SoundUtil.error(player);
            }
            return;
        }
        String shopId = ItemUtil.getShopTicketId(plugin, item);
        if (shopId != null) {
            event.setCancelled(true);
            Location loc = player.getTargetBlockExact(6) != null ? player.getTargetBlockExact(6).getLocation().add(0.5, 1, 0.5) : player.getLocation();
            try {
                if (shopService.spawn(shopId, loc) == null) {
                    messageUtil.send(player, "§c상점이 존재하지 않습니다.");
                    return;
                }
                if (item != null) item.setAmount(item.getAmount() - 1);
                messageUtil.send(player, "§f상점을 소환했습니다.");
            } catch (SQLException e) {
                messageUtil.send(player, "§c상점 소환 실패");
            }
        }
    }
}
