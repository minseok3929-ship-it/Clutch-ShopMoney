package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.gui.*;
import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.service.ChatInputService;
import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.List;

public class InventoryListener implements Listener {
    private static final List<Integer> SHOP_INNER_SLOTS = List.of(
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    );

    private final MoneyService moneyService;
    private final ShopService shopService;
    private final GuiFactory guiFactory;
    private final MessageUtil messageUtil;
    private final ChatInputService chatInputService;

    public InventoryListener(MoneyService moneyService, ShopService shopService, GuiFactory guiFactory, MessageUtil messageUtil, ChatInputService chatInputService) {
        this.moneyService = moneyService;
        this.shopService = shopService;
        this.guiFactory = guiFactory;
        this.messageUtil = messageUtil;
        this.chatInputService = chatInputService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getView().getTopInventory().getHolder() instanceof ShopGuiHolder holder) {
            handlePlayerShop(event, player, holder);
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof AdminSpawnGuiHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            player.getInventory().addItem(clicked.clone());
            messageUtil.send(player, "§f소환권 지급 완료");
            SoundUtil.success(player);
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof AdminEditListGuiHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) return;
            String name = clicked.getItemMeta().getDisplayName().replace("§e", "");
            Shop shop = shopService.all().stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
            if (shop != null) player.openInventory(guiFactory.editShop(shop, 0));
            return;
        }

        if (event.getView().getTopInventory().getHolder() instanceof AdminEditShopGuiHolder holder) {
            handleAdminEdit(event, player, holder);
        }
    }

    private void handlePlayerShop(InventoryClickEvent event, Player player, ShopGuiHolder holder) {
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        Shop shop = shopService.get(holder.shopId());
        if (shop == null) return;

        int slot = event.getSlot();
        if (slot == 45) {
            int nextPage = Math.max(0, holder.page() - 1);
            player.openInventory(guiFactory.playerShop(shop, nextPage));
            return;
        }
        if (slot == 53) {
            player.openInventory(guiFactory.playerShop(shop, holder.page() + 1));
            return;
        }

        if (!SHOP_INNER_SLOTS.contains(slot)) return;

        int indexInPage = SHOP_INNER_SLOTS.indexOf(slot);
        int absoluteIndex = holder.page() * SHOP_INNER_SLOTS.size() + indexInPage;
        if (absoluteIndex < 0 || absoluteIndex >= shop.getItems().size()) return;
        ShopItem shopItem = shop.getItems().get(absoluteIndex);

        ClickType click = event.getClick();
        try {
            if (click == ClickType.LEFT) {
                processBuy(player, shop, shopItem, 1);
            } else if (click == ClickType.SHIFT_LEFT) {
                processBuy(player, shop, shopItem, 64);
            } else if (click == ClickType.RIGHT) {
                processSell(player, shop, shopItem, false);
            } else if (click == ClickType.SHIFT_RIGHT) {
                processSell(player, shop, shopItem, true);
            } else {
                return;
            }
            player.openInventory(guiFactory.playerShop(shop, holder.page()));
        } catch (SQLException e) {
            messageUtil.send(player, "§c거래 처리 실패");
            SoundUtil.error(player);
        }
    }

    private void processBuy(Player player, Shop shop, ShopItem shopItem, int amount) throws SQLException {
        long cost = shopService.currentBuy(shop, shopItem) * amount;
        if (!moneyService.withdraw(player.getUniqueId(), cost)) {
            messageUtil.send(player, "§c돈이 부족합니다!!");
            SoundUtil.error(player);
            return;
        }

        ItemStack give = shopItem.getItem().clone();
        give.setAmount(amount);
        if (player.getInventory().firstEmpty() == -1) {
            moneyService.deposit(player.getUniqueId(), cost);
            messageUtil.send(player, "§c빈 공간이 없습니다!");
            SoundUtil.error(player);
            return;
        }
        player.getInventory().addItem(give);
        messageUtil.send(player, "§f구매 완료");
        SoundUtil.success(player);
    }

    private void processSell(Player player, Shop shop, ShopItem shopItem, boolean all) throws SQLException {
        int sellAmount = all ? countAndRemoveAll(player.getInventory(), shopItem.getItem()) : removeOne(player.getInventory(), shopItem.getItem());
        if (sellAmount <= 0) {
            messageUtil.send(player, "§c물품이 부족합니다!!");
            SoundUtil.error(player);
            return;
        }
        long income = shopService.currentSell(shop, shopItem) * sellAmount;
        moneyService.deposit(player.getUniqueId(), income);
        messageUtil.send(player, "§f판매 완료: §e" + sellAmount + "개");
        SoundUtil.success(player);
    }

    private int removeOne(Inventory inv, ItemStack target) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (shopService.isSameTradeItem(item, target)) {
                item.setAmount(item.getAmount() - 1);
                if (item.getAmount() <= 0) inv.setItem(i, null);
                return 1;
            }
        }
        return 0;
    }

    private int countAndRemoveAll(Inventory inv, ItemStack target) {
        int total = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (shopService.isSameTradeItem(item, target)) {
                total += item.getAmount();
                inv.setItem(i, null);
            }
        }
        return total;
    }

    private void handleAdminEdit(InventoryClickEvent event, Player player, AdminEditShopGuiHolder holder) {
        Shop shop = shopService.get(holder.shopId());
        if (shop == null) {
            event.setCancelled(true);
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != top) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (event.getSlot() == 49) {
            player.closeInventory();
            chatInputService.startVolatilitySession(player, shop.getId(), holder.page());
            return;
        }

        ShopItem selected = shop.getItems().stream().filter(i -> i.getSlot() == event.getSlot()).findFirst().orElse(null);

        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (selected == null) return;
            shop.getItems().remove(selected);
            try {
                shopService.repository().deleteShopItem(selected.getId());
                top.setItem(event.getSlot(), null);
                messageUtil.send(player, "§f아이템 제거 완료");
                SoundUtil.success(player);
            } catch (SQLException e) {
                messageUtil.send(player, "§c아이템 제거 실패");
                SoundUtil.error(player);
            }
            return;
        }

        if (event.getClick() == ClickType.RIGHT) {
            if (selected == null) return;
            player.closeInventory();
            chatInputService.startPriceSession(player, shop.getId(), selected.getSlot(), holder.page());
            return;
        }

        if (selected == null && event.getCursor() != null && event.getCursor().getType() != Material.AIR
                && (event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE || event.getAction() == InventoryAction.PLACE_SOME)) {
            ItemStack clone = event.getCursor().clone();
            clone.setAmount(1);
            ShopItem item = new ShopItem(0, clone, 1, 1, 1, 1, event.getSlot());
            shop.getItems().add(item);
            top.setItem(event.getSlot(), clone);
            try {
                shopService.repository().addShopItem(shop.getId(), item);
                messageUtil.send(player, "§f아이템 등록 완료. 가격을 입력해주세요.");
                SoundUtil.success(player);
                player.closeInventory();
                chatInputService.startPriceSession(player, shop.getId(), item.getSlot(), holder.page());
            } catch (SQLException e) {
                messageUtil.send(player, "§c아이템 등록 실패");
                SoundUtil.error(player);
            }
        }
    }
}
