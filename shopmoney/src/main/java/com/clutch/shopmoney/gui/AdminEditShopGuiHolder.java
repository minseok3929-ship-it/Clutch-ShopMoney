package com.clutch.shopmoney.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public record AdminEditShopGuiHolder(String shopId, int page) implements InventoryHolder {
    @Override
    public Inventory getInventory() { return null; }
}
