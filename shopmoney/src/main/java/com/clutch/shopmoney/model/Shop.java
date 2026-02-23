package com.clutch.shopmoney.model;

import java.util.ArrayList;
import java.util.List;

public class Shop {
    private final String id;
    private String name;
    private boolean fluctuationEnabled;
    private int fluctuationPercent;
    private int periodMinutes;
    private final List<ShopItem> items = new ArrayList<>();

    public Shop(String id, String name, boolean fluctuationEnabled, int fluctuationPercent, int periodMinutes) {
        this.id = id;
        this.name = name;
        this.fluctuationEnabled = fluctuationEnabled;
        this.fluctuationPercent = fluctuationPercent;
        this.periodMinutes = periodMinutes;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isFluctuationEnabled() { return fluctuationEnabled; }
    public void setFluctuationEnabled(boolean fluctuationEnabled) { this.fluctuationEnabled = fluctuationEnabled; }
    public int getFluctuationPercent() { return fluctuationPercent; }
    public void setFluctuationPercent(int fluctuationPercent) { this.fluctuationPercent = fluctuationPercent; }
    public int getPeriodMinutes() { return periodMinutes; }
    public void setPeriodMinutes(int periodMinutes) { this.periodMinutes = periodMinutes; }
    public List<ShopItem> getItems() { return items; }
}
