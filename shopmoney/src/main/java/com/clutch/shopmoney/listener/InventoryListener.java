package com.clutch.shopmoney.listener;

import com.clutch.shopmoney.gui.*;
import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.service.ChatInputService;
import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.ItemUtil;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.NumberUtil;
import com.clutch.shopmoney.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;

public class InventoryListener implements Listener {
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
        if (event.getInventory().getHolder() instanceof ShopGuiHolder holder) {
            event.setCancelled(true);
            handlePlayerShop(event, player, holder);
        } else if (event.getInventory().getHolder() instanceof AdminSpawnGuiHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;
            player.getInventory().addItem(clicked.clone());
            messageUtil.send(player, "§f소환권 지급 완료");
        } else if (event.getInventory().getHolder() instanceof AdminEditListGuiHolder) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || clicked.getItemMeta() == null) return;
            String name = clicked.getItemMeta().getDisplayName().replace("§e", "");
            Shop shop = shopService.all().stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
            if (shop != null) player.openInventory(guiFactory.editShop(shop));
        } else if (event.getInventory().getHolder() instanceof AdminEditShopGuiHolder holder) {
            handleAdminEdit(event, player, holder);
        }
    }

    private void handlePlayerShop(InventoryClickEvent event, Player player, ShopGuiHolder holder) {
        Shop shop = shopService.get(holder.shopId());
        if (shop == null) return;
        int slot = event.getSlot();
        if (slot == 45) {
            player.openInventory(guiFactory.playerShop(shop, Math.max(0, holder.page() - 1)));
            return;
        }
        if (slot == 53) {
            player.openInventory(guiFactory.playerShop(shop, holder.page() + 1));
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType() == Material.AIR) return;
        ShopItem shopItem = shop.getItems().stream().filter(i -> ItemUtil.isSameForSelling(i.getItem(), clicked)).findFirst().orElse(null);
        if (shopItem == null) return;
        boolean buy = event.getClick().isLeftClick();
        int amount = event.getClick().isShiftClick() ? 64 : 1;
        try {
            if (buy) {
                long cost = shopService.currentBuy(shop, shopItem) * amount;
                if (!moneyService.withdraw(player.getUniqueId(), cost)) {
                    messageUtil.send(player, "§f돈이 부족합니다!!");
                    SoundUtil.error(player);
                    return;
                }
                ItemStack give = shopItem.getItem().clone(); give.setAmount(amount);
                if (player.getInventory().firstEmpty() == -1) {
                    moneyService.deposit(player.getUniqueId(), cost);
                    messageUtil.send(player, "§c빈 공간이 없습니다!");
                    SoundUtil.error(player);
                    return;
                }
                player.getInventory().addItem(give);
                messageUtil.send(player, "§f구매 완료");
            } else {
                int sellAmount = event.getClick() == ClickType.SHIFT_RIGHT ? countAndRemoveAll(player.getInventory(), shopItem.getItem()) : removeOne(player.getInventory(), shopItem.getItem());
                if (sellAmount <= 0) {
                    messageUtil.send(player, "§f물품이 부족합니다!!");
                    SoundUtil.error(player);
                    return;
                }
                moneyService.deposit(player.getUniqueId(), shopService.currentSell(shop, shopItem) * sellAmount);
                messageUtil.send(player, "§f판매 완료: " + sellAmount + "개");
            }
        } catch (SQLException e) {
            messageUtil.send(player, "§c거래 처리 실패");
        }
    }

    private int removeOne(Inventory inv, ItemStack target) {
        for (int i=0;i<inv.getSize();i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (ItemUtil.isSameForSelling(item, target)) {
                item.setAmount(item.getAmount()-1);
                if (item.getAmount() <= 0) inv.setItem(i, null);
                return 1;
            }
        }
        return 0;
    }

    private int countAndRemoveAll(Inventory inv, ItemStack target) {
        int total = 0;
        for (int i=0;i<inv.getSize();i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (ItemUtil.isSameForSelling(item, target)) {
                total += item.getAmount();
                inv.setItem(i, null);
            }
        }
        return total;
    }

    private void handleAdminEdit(InventoryClickEvent event, Player player, AdminEditShopGuiHolder holder) {
        Shop shop = shopService.get(holder.shopId());
        if (shop == null) return;
        if (event.getSlot() == 49) {
            event.setCancelled(true);
            player.closeInventory();
            messageUtil.send(player, "§f채팅에 '변동률 주기(분)' 입력 (예: 20 10)");
            chatInputService.waitFor(player.getUniqueId(), input -> {
                String[] split = input.split(" ");
                if (split.length != 2) { messageUtil.send(player, "§c형식 오류"); return; }
                Long p = NumberUtil.parsePositiveLong(split[0].replace("-", ""));
                Long m = NumberUtil.parsePositiveLong(split[1]);
                if (p == null || m == null) { messageUtil.send(player, "§c숫자 오류"); return; }
                int perc = Integer.parseInt(split[0]);
                if (perc < -50 || perc > 80) { messageUtil.send(player, "§c변동률 범위 오류(-50~80)"); return; }
                shop.setFluctuationPercent(perc);
                shop.setPeriodMinutes(m.intValue());
                try { shopService.repository().updateShopMeta(shop); } catch (SQLException ignored) {}
                messageUtil.send(player, "§f변동률/주기 적용 완료");
            });
            return;
        }
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            event.setCancelled(true);
            ShopItem toRemove = shop.getItems().stream().filter(i -> i.getSlot() == event.getSlot()).findFirst().orElse(null);
            if (toRemove != null) {
                shop.getItems().remove(toRemove);
                try { shopService.repository().deleteShopItem(toRemove.getId()); } catch (SQLException ignored) {}
                event.getInventory().setItem(event.getSlot(), null);
                messageUtil.send(player, "§f아이템 제거 완료");
            }
            return;
        }
        if (event.getClick().isRightClick() && event.getCurrentItem() != null) {
            event.setCancelled(true);
            ShopItem target = shop.getItems().stream().filter(i -> i.getSlot() == event.getSlot()).findFirst().orElse(null);
            if (target == null) return;
            player.closeInventory();
            messageUtil.send(player, "§f채팅에 '구매가 판매가' 입력 (예: 1200 800)");
            chatInputService.waitFor(player.getUniqueId(), input -> {
                String[] split = input.split(" ");
                if (split.length != 2) { messageUtil.send(player, "§c형식 오류"); return; }
                Long buy = NumberUtil.parsePositiveLong(split[0]);
                Long sell = NumberUtil.parsePositiveLong(split[1]);
                if (buy == null || sell == null) { messageUtil.send(player, "§c숫자 오류"); return; }
                target.setPreviousBuyPrice(target.getBuyPrice());
                target.setPreviousSellPrice(target.getSellPrice());
                target.setBuyPrice(buy); target.setSellPrice(sell);
                try { shopService.repository().updateShopItem(target); } catch (SQLException ignored) {}
                messageUtil.send(player, "§f가격 설정 완료");
            });
            return;
        }
        if (event.getClickedInventory() == player.getInventory() && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            ItemStack clone = event.getCurrentItem().clone(); clone.setAmount(1);
            int slot = event.getSlot();
            event.setCancelled(false);
            if (event.getView().getTopInventory().getItem(slot) == null) {
                chatInputService.waitFor(player.getUniqueId(), input -> {
                    String[] split = input.split(" ");
                    if (split.length != 2) { messageUtil.send(player, "§c형식 오류"); return; }
                    Long buy = NumberUtil.parsePositiveLong(split[0]); Long sell = NumberUtil.parsePositiveLong(split[1]);
                    if (buy == null || sell == null) { messageUtil.send(player, "§c숫자 오류"); return; }
                    ShopItem item = new ShopItem(0, clone, buy, sell, buy, sell, slot);
                    shop.getItems().add(item);
                    try { shopService.repository().addShopItem(shop.getId(), item); } catch (SQLException ignored) {}
                    messageUtil.send(player, "§f아이템 등록 완료");
                });
                messageUtil.send(player, "§f채팅에 '구매가 판매가' 입력");
            }
        }
    }
}
