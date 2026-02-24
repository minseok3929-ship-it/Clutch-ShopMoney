package com.clutch.shopmoney.model;

public enum ShopMode {
    BUY_SELL,
    BUY_ONLY,
    SELL_ONLY;

    public ShopMode next() {
        return switch (this) {
            case BUY_SELL -> BUY_ONLY;
            case BUY_ONLY -> SELL_ONLY;
            case SELL_ONLY -> BUY_SELL;
        };
    }
}
