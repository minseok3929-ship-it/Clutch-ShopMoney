package com.clutch.shopmoney.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ItemUtil {
    public static final String CASH_AMOUNT_KEY = "clutch_cash_amount";
    public static final String SHOP_ID_KEY = "clutch_shop_id";
    public static final String SHOP_TICKET_KEY = "clutch_shop_ticket";
    public static final String SHOP_ITEM_ID_KEY = "clutch_shop_item_id";

    private ItemUtil() {}

    public static ItemStack createCash(JavaPlugin plugin, long amount, int cmd) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§6현금 " + amount);
        meta.setCustomModelData(cmd);
        List<String> lore = new ArrayList<>();
        lore.add("§7우클릭 시 계좌에 입금됩니다.");
        lore.add("§7금액: §f" + amount);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, CASH_AMOUNT_KEY), PersistentDataType.LONG, amount);
        item.setItemMeta(meta);
        return item;
    }

    public static Long getCashAmount(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(new NamespacedKey(plugin, CASH_AMOUNT_KEY), PersistentDataType.LONG);
    }

    public static ItemStack createShopTicket(JavaPlugin plugin, String shopId, String name, String loreLine) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName("§e상점 소환권: " + name);
        meta.setLore(List.of("§7" + loreLine, "§7우클릭: 상점 소환"));
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, SHOP_TICKET_KEY), PersistentDataType.STRING, shopId);
        item.setItemMeta(meta);
        return item;
    }

    public static String getShopTicketId(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PAPER || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, SHOP_TICKET_KEY), PersistentDataType.STRING);
    }

    public static void applyShopItemId(JavaPlugin plugin, ItemStack item, String id) {
        if (item == null || id == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, SHOP_ITEM_ID_KEY), PersistentDataType.STRING, id);
        item.setItemMeta(meta);
    }

    public static String getShopItemId(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(new NamespacedKey(plugin, SHOP_ITEM_ID_KEY), PersistentDataType.STRING);
    }

    public static boolean isSameForSelling(JavaPlugin plugin, ItemStack template, ItemStack candidate, String shopItemId) {
        if (template == null || candidate == null || template.getType() != candidate.getType()) return false;
        String id = getShopItemId(plugin, candidate);
        if (shopItemId != null && shopItemId.equals(id)) return true;

        ItemMeta ta = template.getItemMeta();
        ItemMeta ca = candidate.getItemMeta();
        if (ta == null && ca == null) return true;
        if (ta == null || ca == null) return false;

        if (ta.hasCustomModelData()) {
            if (!ca.hasCustomModelData()) return false;
            if (ta.getCustomModelData() != ca.getCustomModelData()) return false;
        }

        if (ta.hasDisplayName()) {
            if (!ca.hasDisplayName()) return false;
            if (!Objects.equals(ta.getDisplayName(), ca.getDisplayName())) return false;
        }

        return true;
    }
}
