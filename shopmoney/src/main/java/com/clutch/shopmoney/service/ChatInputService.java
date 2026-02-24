package com.clutch.shopmoney.service;

import com.clutch.shopmoney.gui.GuiFactory;
import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInputService {
    private final JavaPlugin plugin;
    private final ShopService shopService;
    private final GuiFactory guiFactory;
    private final MessageUtil messageUtil;
    private final Map<UUID, ChatSession> sessions = new ConcurrentHashMap<>();

    public ChatInputService(JavaPlugin plugin, ShopService shopService, GuiFactory guiFactory, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.shopService = shopService;
        this.guiFactory = guiFactory;
        this.messageUtil = messageUtil;
    }

    public void startPriceSession(Player player, String shopId, int targetSlot, int page) {
        startSession(player, new ChatSession(SessionType.PRICE_SET, shopId, targetSlot, page, null));
        messageUtil.send(player, "§f구매가와 판매가를 입력하세요. 예) 1200 800 (숫자만)");
    }

    public void startVolatilitySession(Player player, String shopId, int page) {
        startSession(player, new ChatSession(SessionType.VOLATILITY_SET, shopId, null, page, null));
        messageUtil.send(player, "§f변동률 범위를 입력하세요. 예) -50~80, -50 ~ 80, -50 80");
    }

    private void startSession(Player player, ChatSession session) {
        ChatSession old = sessions.remove(player.getUniqueId());
        if (old != null && old.timeoutTask() != null) old.timeoutTask().cancel();
        BukkitTask timeoutTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ChatSession active = sessions.remove(player.getUniqueId());
            if (active != null) {
                messageUtil.send(player, "§c취소되었습니다");
                reopen(player, active.shopId(), active.page());
            }
        }, 20L * 30);
        sessions.put(player.getUniqueId(), session.withTimeout(timeoutTask));
    }

    public boolean handleChat(Player player, String message) {
        ChatSession session = sessions.remove(player.getUniqueId());
        if (session == null) return false;
        if (session.timeoutTask() != null) session.timeoutTask().cancel();
        Bukkit.getScheduler().runTask(plugin, () -> process(player, session, message.trim()));
        return true;
    }

    private void process(Player player, ChatSession session, String input) {
        Shop shop = shopService.get(session.shopId());
        if (shop == null) {
            messageUtil.send(player, "§c취소되었습니다");
            return;
        }

        switch (session.type()) {
            case PRICE_SET -> {
                String[] split = input.split("\\s+");
                if (split.length != 2) { cancelAndReturn(player, shop, session.page()); return; }
                Long buy = NumberUtil.parsePositiveLong(split[0]);
                Long sell = NumberUtil.parsePositiveLong(split[1]);
                if (buy == null || sell == null) { cancelAndReturn(player, shop, session.page()); return; }
                ShopItem item = shop.getItems().stream().filter(it -> it.getSlot() == (session.targetItemId() == null ? -1 : session.targetItemId())).findFirst().orElse(null);
                if (item == null) { cancelAndReturn(player, shop, session.page()); return; }
                item.setBuyPrice(buy);
                item.setSellPrice(sell);
                item.setPreviousBuyPrice(item.getCurrentBuyPrice());
                item.setPreviousSellPrice(item.getCurrentSellPrice());
                item.setCurrentBuyPrice(buy);
                item.setCurrentSellPrice(sell);
                shopService.normalizeItemPrices(item);
                try {
                    shopService.repository().replaceItems(shop.getId(), shop.getItems());
                    messageUtil.send(player, "§f설정 완료!");
                } catch (SQLException e) {
                    messageUtil.send(player, "§c취소되었습니다");
                }
                reopen(player, shop.getId(), session.page());
            }
            case VOLATILITY_SET -> {
                int[] range = parseRange(input);
                if (range == null || range[0] < -50 || range[1] > 80 || range[0] > range[1]) {
                    cancelAndReturn(player, shop, session.page());
                    return;
                }
                shop.setVolatilityMinPercent(range[0]);
                shop.setVolatilityMaxPercent(range[1]);
                try { shopService.repository().updateShopMeta(shop); } catch (SQLException e) { cancelAndReturn(player, shop, session.page()); return; }
                startSession(player, new ChatSession(SessionType.PERIOD_SET, shop.getId(), null, session.page(), null));
                messageUtil.send(player, "§f주기를 분 단위로 입력하세요. 예) 5 (숫자만)");
            }
            case PERIOD_SET -> {
                Integer period = parseInt(input);
                if (period == null || period < 1) { cancelAndReturn(player, shop, session.page()); return; }
                shop.setPeriodMinutes(period);
                try {
                    shopService.repository().updateShopMeta(shop);
                    messageUtil.send(player, "§f설정 완료!");
                } catch (SQLException e) {
                    messageUtil.send(player, "§c취소되었습니다");
                }
                reopen(player, shop.getId(), session.page());
            }
        }
    }

    private int[] parseRange(String input) {
        String s = input.trim();
        if (s.contains("~")) {
            String[] p = s.split("~");
            if (p.length != 2) return null;
            Integer a = parseInt(p[0].trim());
            Integer b = parseInt(p[1].trim());
            return (a == null || b == null) ? null : new int[]{a,b};
        }
        if (s.contains(",")) {
            String[] p = s.split(",");
            if (p.length != 2) return null;
            Integer a = parseInt(p[0].trim());
            Integer b = parseInt(p[1].trim());
            return (a == null || b == null) ? null : new int[]{a,b};
        }
        String[] p = s.split("\\s+");
        if (p.length != 2) return null;
        Integer a = parseInt(p[0]);
        Integer b = parseInt(p[1]);
        return (a == null || b == null) ? null : new int[]{a,b};
    }

    private Integer parseInt(String input) { try { return Integer.parseInt(input); } catch (NumberFormatException e) { return null; } }

    private void cancelAndReturn(Player player, Shop shop, int page) {
        messageUtil.send(player, "§c취소되었습니다");
        reopen(player, shop.getId(), page);
    }

    private void reopen(Player player, String shopId, int page) {
        Shop shop = shopService.get(shopId);
        if (shop == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(guiFactory.editShop(shop, page)));
    }

    public enum SessionType { PRICE_SET, VOLATILITY_SET, PERIOD_SET }

    private record ChatSession(SessionType type, String shopId, Integer targetItemId, int page, BukkitTask timeoutTask) {
        private ChatSession withTimeout(BukkitTask timeoutTask) { return new ChatSession(type, shopId, targetItemId, page, timeoutTask); }
    }
}
