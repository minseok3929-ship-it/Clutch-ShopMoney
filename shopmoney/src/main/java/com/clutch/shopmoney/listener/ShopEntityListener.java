package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.gui.GuiFactory;
import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.MessageUtil;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class ShopEntityListener implements Listener {
    private final ShopService shopService;
    private final GuiFactory guiFactory;
    private final MessageUtil messageUtil;

    public ShopEntityListener(ShopService shopService, GuiFactory guiFactory, MessageUtil messageUtil) {
        this.shopService = shopService;
        this.guiFactory = guiFactory;
        this.messageUtil = messageUtil;
    }

    @EventHandler
    public void onShopInteract(PlayerInteractEntityEvent event) {
        String shopId = shopService.findShopId(event.getRightClicked());
        if (shopId == null) return;
        event.setCancelled(true);
        Shop shop = shopService.get(shopId);
        if (shop == null) {
            messageUtil.send(event.getPlayer(), "§c상점 데이터를 찾을 수 없습니다.");
            return;
        }
        event.getPlayer().openInventory(guiFactory.playerShop(shop, 0));
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        String shopId = shopService.findShopId(villager);
        if (shopId != null) event.setCancelled(true);
    }
}
