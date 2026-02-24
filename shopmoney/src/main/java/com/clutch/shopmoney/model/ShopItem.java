package com.clutch.shopmoney.model;

import org.bukkit.inventory.ItemStack;

public class ShopItem {
    private final int id;
    private ItemStack item;
    private long buyPrice; // base
    private long sellPrice; // base
    private long previousBuyPrice;
    private long previousSellPrice;
    private long currentBuyPrice;
    private long currentSellPrice;
    private int slot;

    public ShopItem(int id, ItemStack item, long buyPrice, long sellPrice, long previousBuyPrice, long previousSellPrice, long currentBuyPrice, long currentSellPrice, int slot) {
        this.id = id;
        this.item = item;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.previousBuyPrice = previousBuyPrice;
        this.previousSellPrice = previousSellPrice;
        this.currentBuyPrice = currentBuyPrice;
        this.currentSellPrice = currentSellPrice;
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
    public long getCurrentBuyPrice() { return currentBuyPrice; }
    public void setCurrentBuyPrice(long currentBuyPrice) { this.currentBuyPrice = currentBuyPrice; }
    public long getCurrentSellPrice() { return currentSellPrice; }
    public void setCurrentSellPrice(long currentSellPrice) { this.currentSellPrice = currentSellPrice; }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
}
