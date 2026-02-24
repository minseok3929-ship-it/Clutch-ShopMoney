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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.Set;

public class InventoryListener implements Listener {
    private static final Set<Integer> EDIT_LOCKED = Set.of(45, 49, 50, 53);

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
        Inventory top = event.getView().getTopInventory();

        if (top.getHolder() instanceof ShopGuiHolder holder) {
            handlePlayerShop(event, player, holder);
            return;
        }

        if (top.getHolder() instanceof AdminEditShopGuiHolder holder) {
            handleEdit(event, player, holder);
            return;
        }

        if (top.getHolder() instanceof AdminSpawnGuiHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != top) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            player.getInventory().addItem(clicked.clone());
            messageUtil.send(player, "§f소환권 지급 완료");
            SoundUtil.success(player);
            return;
        }

        if (top.getHolder() instanceof AdminEditListGuiHolder) {
            event.setCancelled(true);
            if (event.getClickedInventory() != top) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) return;
            String name = clicked.getItemMeta().getDisplayName().replace("§e", "");
            Shop shop = shopService.all().stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
            if (shop != null) player.openInventory(guiFactory.editShop(shop, 0));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder() instanceof ShopGuiHolder) {
            event.setCancelled(true);
            return;
        }
        if (top.getHolder() instanceof AdminEditShopGuiHolder) {
            for (int raw : event.getRawSlots()) {
                if (raw >= top.getSize()) continue;
                if (!ShopService.INNER_SLOTS.contains(raw) || EDIT_LOCKED.contains(raw)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AdminEditShopGuiHolder holder)) return;
        Shop shop = shopService.get(holder.shopId());
        if (shop == null) return;
        shopService.saveEditPage(shop, holder.page(), top);
    }

    private void handlePlayerShop(InventoryClickEvent event, Player player, ShopGuiHolder holder) {
        event.setCancelled(true);
        Inventory top = event.getView().getTopInventory();
        if (event.getClickedInventory() != top) return;

        Shop shop = shopService.get(holder.shopId());
        if (shop == null) return;

        int slot = event.getSlot();
        int total = shopService.totalPages(shop);
        if (slot == 45) { player.openInventory(guiFactory.playerShop(shop, Math.max(0, holder.page() - 1))); return; }
        if (slot == 53) { player.openInventory(guiFactory.playerShop(shop, Math.min(total - 1, holder.page() + 1))); return; }
        if (!ShopService.INNER_SLOTS.contains(slot)) return;

        int absoluteIndex = holder.page() * ShopService.INNER_SLOTS.size() + ShopService.INNER_SLOTS.indexOf(slot);
        ShopItem shopItem = shop.getItems().stream().filter(i -> i.getSlot() == absoluteIndex).findFirst().orElse(null);
        if (shopItem == null) return;

        try {
            switch (event.getClick()) {
                case LEFT -> buy(player, shop, shopItem, 1);
                case SHIFT_LEFT -> buy(player, shop, shopItem, 64);
                case RIGHT -> sell(player, shop, shopItem, false);
                case SHIFT_RIGHT -> sell(player, shop, shopItem, true);
                default -> { return; }
            }
        } catch (SQLException e) {
            messageUtil.send(player, "§c거래 처리 실패");
            SoundUtil.error(player);
        }
        player.openInventory(guiFactory.playerShop(shop, holder.page()));
    }

    private void handleEdit(InventoryClickEvent event, Player player, AdminEditShopGuiHolder holder) {
        Inventory top = event.getView().getTopInventory();
        Shop shop = shopService.get(holder.shopId());
        if (shop == null) { event.setCancelled(true); return; }

        if (event.getClickedInventory() == top) {
            int slot = event.getSlot();
            int total = Math.max(1, (int) Math.ceil((shop.getItems().size() + 1) / (double) ShopService.INNER_SLOTS.size()));
            if (slot == 45) { event.setCancelled(true); player.openInventory(guiFactory.editShop(shop, Math.max(0, holder.page() - 1))); return; }
            if (slot == 53) { event.setCancelled(true); player.openInventory(guiFactory.editShop(shop, Math.min(total - 1, holder.page() + 1))); return; }
            if (slot == 50) { event.setCancelled(true); player.closeInventory(); chatInputService.startVolatilitySession(player, shop.getId(), holder.page()); return; }
            if (EDIT_LOCKED.contains(slot) || !ShopService.INNER_SLOTS.contains(slot)) { event.setCancelled(true); return; }

            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                top.setItem(slot, null);
                messageUtil.send(player, "§f아이템 제거 완료");
                return;
            }
            if (event.getClick() == ClickType.RIGHT && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                event.setCancelled(true);
                player.closeInventory();
                chatInputService.startPriceSession(player, shop.getId(), holder.page() * ShopService.INNER_SLOTS.size() + ShopService.INNER_SLOTS.indexOf(slot), holder.page());
                return;
            }
            // allow normal place/move in editable slots
            return;
        }

        // allow shift move from player inv, but block insertion into locked slots by canceling only if top full/invalid not tracked by API reliably
    }

    private void buy(Player player, Shop shop, ShopItem item, int amount) throws SQLException {
        long cost = shopService.currentBuy(shop, item) * amount;
        if (!moneyService.withdraw(player.getUniqueId(), cost)) { messageUtil.send(player, "§c돈이 부족합니다!!"); SoundUtil.error(player); return; }
        if (player.getInventory().firstEmpty() == -1) { moneyService.deposit(player.getUniqueId(), cost); messageUtil.send(player, "§c빈 공간이 없습니다!"); SoundUtil.error(player); return; }
        ItemStack give = item.getItem().clone(); give.setAmount(amount); player.getInventory().addItem(give);
        messageUtil.send(player, "§f구매 완료"); SoundUtil.success(player);
    }

    private void sell(Player player, Shop shop, ShopItem target, boolean all) throws SQLException {
        int sold = all ? countAndRemoveAll(player.getInventory(), target.getItem()) : removeOne(player.getInventory(), target.getItem());
        if (sold <= 0) { messageUtil.send(player, "§c물품이 부족합니다!!"); SoundUtil.error(player); return; }
        moneyService.deposit(player.getUniqueId(), shopService.currentSell(shop, target) * sold);
        messageUtil.send(player, "§f판매 완료: §e" + sold + "개");
        SoundUtil.success(player);
    }

    private int removeOne(Inventory inv, ItemStack target) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && shopService.isSameTradeItem(item, target)) {
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
            if (item != null && shopService.isSameTradeItem(item, target)) {
                total += item.getAmount();
                inv.setItem(i, null);
            }
        }
        return total;
    }
}
