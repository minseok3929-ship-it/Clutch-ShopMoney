package com.clutch.shopmoney.gui;

import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class GuiFactory {
    private final JavaPlugin plugin;
    private final ShopService shopService;

    public GuiFactory(JavaPlugin plugin, ShopService shopService) {
        this.plugin = plugin;
        this.shopService = shopService;
    }

    public Inventory playerShop(Shop shop, int page) {
        int totalPages = shopService.totalPages(shop);
        int normalized = Math.max(0, Math.min(page, totalPages - 1));
        Inventory inv = Bukkit.createInventory(new ShopGuiHolder(shop.getId(), normalized), 54, plugin.getConfig().getString("shop.gui.title", "§8상점") + " §7- " + shop.getName());
        fillBorder(inv);
        int start = normalized * ShopService.INNER_SLOTS.size();
        for (int i = 0; i < ShopService.INNER_SLOTS.size(); i++) {
            int idx = start + i;
            if (idx >= shop.getItems().size()) break;
            ShopItem si = shop.getItems().get(idx);
            ItemStack display = shopService.buildDisplayItem(shop, si);
            inv.setItem(ShopService.INNER_SLOTS.get(i), display);
        }
        inv.setItem(45, nav("§a이전 페이지"));
        inv.setItem(49, pageItem(normalized + 1, totalPages));
        inv.setItem(53, nav("§a다음 페이지"));
        return inv;
    }

    public Inventory spawnTickets(Iterable<Shop> shops) {
        Inventory inv = Bukkit.createInventory(new AdminSpawnGuiHolder(), 54, plugin.getConfig().getString("shop.spawnGui.title", "§8상점 소환권"));
        int slot = 0;
        for (Shop s : shops) {
            if (slot >= 54) break;
            inv.setItem(slot++, ItemUtil.createShopTicket(plugin, s.getId(), s.getName(), "변동률: " + s.getVolatilityMinPercent() + "~" + s.getVolatilityMaxPercent() + "% / 품목: " + s.getItems().size()));
        }
        return inv;
    }

    public Inventory editList(Iterable<Shop> shops) {
        Inventory inv = Bukkit.createInventory(new AdminEditListGuiHolder(), 54, "§8상점 선택");
        int slot = 0;
        for (Shop s : shops) {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + s.getName());
                meta.setLore(List.of("§7아이템 수: " + s.getItems().size(), "§e클릭해서 편집"));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        return inv;
    }

    public Inventory editShop(Shop shop) { return editShop(shop, 0); }

    public Inventory editShop(Shop shop, int page) {
        int totalPages = Math.max(1, (int) Math.ceil((shop.getItems().size() + 1) / (double) ShopService.INNER_SLOTS.size()));
        int normalized = Math.max(0, Math.min(page, totalPages - 1));
        Inventory inv = Bukkit.createInventory(new AdminEditShopGuiHolder(shop.getId(), normalized), 54, plugin.getConfig().getString("shop.editGui.title", "§8상점 편집") + " §7- " + shop.getName());
        fillBorder(inv);

        int start = normalized * ShopService.INNER_SLOTS.size();
        for (int i = 0; i < ShopService.INNER_SLOTS.size(); i++) {
            int absolute = start + i;
            ShopItem existing = shop.getItems().stream().filter(it -> it.getSlot() == absolute).findFirst().orElse(null);
            if (existing != null) {
                ItemStack display = existing.getItem().clone();
                inv.setItem(ShopService.INNER_SLOTS.get(i), display);
            }
        }

        inv.setItem(45, nav("§a이전 페이지"));
        inv.setItem(48, modeItem(shop));
        inv.setItem(49, pageItem(normalized + 1, totalPages));
        inv.setItem(50, nav("§b변동률/주기 설정 (채팅)"));
        inv.setItem(53, nav("§a다음 페이지"));
        return inv;
    }


    private ItemStack modeItem(Shop shop) {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f상점 모드: §e" + shop.getShopMode().name());
            meta.setLore(List.of(
                    "§7현재: " + shop.getShopMode().name(),
                    "§7BUY_ONLY: 좌클릭 구매만",
                    "§7SELL_ONLY: 우클릭 판매만",
                    "§7BUY_SELL: 구매+판매",
                    "§e클릭: 모드 순환 토글"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) { gm.setDisplayName(" "); glass.setItemMeta(gm); }
        for (int i = 0; i < 54; i++) {
            int row = i / 9; int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, glass);
        }
    }

    private ItemStack pageItem(int current, int total) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f페이지 §e" + current + "/" + total);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack nav(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }
}
