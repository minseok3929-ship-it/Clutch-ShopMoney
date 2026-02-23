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
        Inventory inv = Bukkit.createInventory(new ShopGuiHolder(shop.getId(), page), 54, plugin.getConfig().getString("shop.gui.title", "§8상점") + " §7- " + shop.getName());
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        if (gm != null) { gm.setDisplayName(" "); glass.setItemMeta(gm); }
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(i, glass);
        }
        List<Integer> inside = new ArrayList<>();
        for (int r=1;r<=4;r++) for(int c=1;c<=7;c++) inside.add(r*9+c);
        int start = page * inside.size();
        for (int i=0;i<inside.size();i++) {
            int idx = start + i;
            if (idx >= shop.getItems().size()) break;
            ShopItem si = shop.getItems().get(idx);
            ItemStack display = si.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§7이전 구매가: §f" + si.getPreviousBuyPrice());
                lore.add("§7현재 구매가: §f" + shopService.currentBuy(shop, si));
                lore.add("§7이전 판매가: §f" + si.getPreviousSellPrice());
                lore.add("§7현재 판매가: §f" + shopService.currentSell(shop, si));
                lore.add("§e좌클릭=1개 구매, Shift+좌클릭=64개 구매");
                lore.add("§e우클릭=1개 판매, Shift+우클릭=전부 판매");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(inside.get(i), display);
        }
        inv.setItem(45, nav("§a이전 페이지"));
        inv.setItem(53, nav("§a다음 페이지"));
        return inv;
    }

    public Inventory spawnTickets(Iterable<Shop> shops) {
        Inventory inv = Bukkit.createInventory(new AdminSpawnGuiHolder(), 54, plugin.getConfig().getString("shop.spawnGui.title", "§8상점 소환권"));
        int slot = 0;
        for (Shop s : shops) {
            if (slot >= 54) break;
            inv.setItem(slot++, ItemUtil.createShopTicket(plugin, s.getId(), s.getName(), "변동률: " + s.getFluctuationPercent() + "% / 품목: " + s.getItems().size()));
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

    public Inventory editShop(Shop shop) {
        Inventory inv = Bukkit.createInventory(new AdminEditShopGuiHolder(shop.getId()), 54, plugin.getConfig().getString("shop.editGui.title", "§8상점 편집") + " §7- " + shop.getName());
        for (ShopItem item : shop.getItems()) {
            if (item.getSlot() >= 0 && item.getSlot() < 54) inv.setItem(item.getSlot(), item.getItem());
        }
        inv.setItem(49, nav("§b변동률/주기 설정 (채팅)"));
        return inv;
    }

    private ItemStack nav(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); item.setItemMeta(meta); }
        return item;
    }
}
