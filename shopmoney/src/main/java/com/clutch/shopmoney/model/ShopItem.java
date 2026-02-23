package com.clutch.shopmoney.model;

import org.bukkit.inventory.ItemStack;

public class ShopItem {
    private final int id;
    private ItemStack item;
    private long buyPrice;
    private long sellPrice;
    private long previousBuyPrice;
    private long previousSellPrice;
    private int slot;

    public ShopItem(int id, ItemStack item, long buyPrice, long sellPrice, long previousBuyPrice, long previousSellPrice, int slot) {
        this.id = id;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.previousBuyPrice = previousBuyPrice;
        this.previousSellPrice = previousSellPrice;
        this.slot = slot;
    }

    public int getId() { return id; }
    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
    public long getBuyPrice() { return buyPrice; }
    public void setBuyPrice(long buyPrice) { this.buyPrice = buyPrice; }
    public long getSellPrice() { return sellPrice; }
    public void setSellPrice(long sellPrice) { this.sellPrice = sellPrice; }
    public long getPreviousBuyPrice() { return previousBuyPrice; }
    public void setPreviousBuyPrice(long previousBuyPrice) { this.previousBuyPrice = previousBuyPrice; }
    public long getPreviousSellPrice() { return previousSellPrice; }
    public void setPreviousSellPrice(long previousSellPrice) { this.previousSellPrice = previousSellPrice; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
}
