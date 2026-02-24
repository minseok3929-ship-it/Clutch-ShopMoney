package com.clutch.shopmoney.model;

import java.util.ArrayList;
import java.util.List;

public class Shop {
    private final String id;
    private String name;
    private boolean fluctuationEnabled;
    private int volatilityMinPercent;
    private int volatilityMaxPercent;
    private int periodMinutes;
    private final List<ShopItem> items = new ArrayList<>();

    public Shop(String id, String name, boolean fluctuationEnabled, int volatilityMinPercent, int volatilityMaxPercent, int periodMinutes) {
        this.id = id;
        this.name = name;
        this.fluctuationEnabled = fluctuationEnabled;
        this.volatilityMinPercent = volatilityMinPercent;
        this.volatilityMaxPercent = volatilityMaxPercent;
        this.periodMinutes = periodMinutes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isFluctuationEnabled() { return fluctuationEnabled; }
    public void setFluctuationEnabled(boolean fluctuationEnabled) { this.fluctuationEnabled = fluctuationEnabled; }
    public int getVolatilityMinPercent() { return volatilityMinPercent; }
    public void setVolatilityMinPercent(int volatilityMinPercent) { this.volatilityMinPercent = volatilityMinPercent; }
    public int getVolatilityMaxPercent() { return volatilityMaxPercent; }
    public void setVolatilityMaxPercent(int volatilityMaxPercent) { this.volatilityMaxPercent = volatilityMaxPercent; }
    public int getPeriodMinutes() { return periodMinutes; }
    public void setPeriodMinutes(int periodMinutes) { this.periodMinutes = periodMinutes; }
    public List<ShopItem> getItems() { return items; }
}
