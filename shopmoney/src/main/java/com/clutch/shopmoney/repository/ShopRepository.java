package com.clutch.shopmoney.repository;

import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.model.ShopMode;
import com.clutch.shopmoney.model.ShopSpawn;
import com.clutch.shopmoney.util.SerializationUtil;

import java.sql.*;
import java.util.*;

public class ShopRepository {
    private ShopMode parseMode(String raw) {
        try { return ShopMode.valueOf(raw); } catch (Exception e) { return ShopMode.BUY_SELL; }
    }
    private final Database database;

    public ShopRepository(Database database) {
        this.database = database;
    }

    public void createShop(String id, String name) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO shops(id,name,fluctuation_enabled,fluctuation_percent,volatility_min,volatility_max,period_minutes,interval_minutes,shop_mode) VALUES(?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setInt(3, 1);
            ps.setInt(4, 0);
            ps.setInt(5, -50);
            ps.setInt(6, 80);
            ps.setInt(7, 10);
            ps.setInt(8, 5);
            ps.setString(9, ShopMode.BUY_SELL.name());
            ps.executeUpdate();
        }
    }

    public boolean deleteShop(String id) throws SQLException {
        try (Connection conn = database.getConnection()) {
            try (PreparedStatement item = conn.prepareStatement("DELETE FROM shop_items WHERE shop_id=?")) { item.setString(1, id); item.executeUpdate(); }
            try (PreparedStatement spawn = conn.prepareStatement("DELETE FROM shop_spawns WHERE shop_id=?")) { spawn.setString(1, id); spawn.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM shops WHERE id=?")) { ps.setString(1, id); return ps.executeUpdate() > 0; }
        }
    }

    public Map<String, Shop> loadAll() throws SQLException {
        Map<String, Shop> map = new LinkedHashMap<>();
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM shops"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int period = rs.getInt("period_minutes");
                int interval = rs.getInt("interval_minutes");
                if (period <= 0) period = interval > 0 ? interval : 5;
                Shop s = new Shop(rs.getString("id"), rs.getString("name"), rs.getInt("fluctuation_enabled") == 1, rs.getInt("volatility_min"), rs.getInt("volatility_max"), period, parseMode(rs.getString("shop_mode")));
                map.put(s.getId(), s);
            }
        }
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_items ORDER BY slot ASC"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Shop shop = map.get(rs.getString("shop_id"));
                if (shop == null) continue;
                long buy = rs.getLong("buy_price");
                long sell = rs.getLong("sell_price");
                long currentBuy = rs.getLong("current_buy");
                long currentSell = rs.getLong("current_sell");
                if (currentBuy <= 0) currentBuy = buy;
                if (currentSell <= 0) currentSell = sell;
                ShopItem item = new ShopItem(
                        rs.getInt("id"),
                        SerializationUtil.itemFromBase64(rs.getString("item_base64")),
                        buy,
                        sell,
                        rs.getLong("previous_buy"),
                        rs.getLong("previous_sell"),
                        currentBuy,
                        currentSell,
                        rs.getInt("slot")
                );
                shop.getItems().add(item);
            }
        }
        return map;
    }

    public void replaceItems(String shopId, List<ShopItem> items) throws SQLException {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM shop_items WHERE shop_id=?")) {
                del.setString(1, shopId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO shop_items(shop_id,item_base64,buy_price,sell_price,previous_buy,previous_sell,current_buy,current_sell,slot) VALUES(?,?,?,?,?,?,?,?,?)")) {
                for (ShopItem item : items) {
                    ins.setString(1, shopId);
                    ins.setString(2, SerializationUtil.itemToBase64(item.getItem()));
                    ins.setLong(3, item.getBuyPrice());
                    ins.setLong(4, item.getSellPrice());
                    ins.setLong(5, item.getPreviousBuyPrice());
                    ins.setLong(6, item.getPreviousSellPrice());
                    ins.setLong(7, item.getCurrentBuyPrice());
                    ins.setLong(8, item.getCurrentSellPrice());
                    ins.setInt(9, item.getSlot());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
            conn.commit();
        }
    }

    public void updateShopItem(ShopItem item) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE shop_items SET item_base64=?, buy_price=?, sell_price=?, previous_buy=?, previous_sell=?, current_buy=?, current_sell=?, slot=? WHERE id=?")) {
            ps.setString(1, SerializationUtil.itemToBase64(item.getItem()));
            ps.setLong(2, item.getBuyPrice());
            ps.setLong(3, item.getSellPrice());
            ps.setLong(4, item.getPreviousBuyPrice());
            ps.setLong(5, item.getPreviousSellPrice());
            ps.setLong(6, item.getCurrentBuyPrice());
            ps.setLong(7, item.getCurrentSellPrice());
            ps.setInt(8, item.getSlot());
            ps.setInt(9, item.getId());
            ps.executeUpdate();
        }
    }

    public void updateShopMeta(Shop shop) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE shops SET name=?, fluctuation_enabled=?, fluctuation_percent=?, volatility_min=?, volatility_max=?, period_minutes=?, interval_minutes=?, shop_mode=? WHERE id=?")) {
            ps.setString(1, shop.getName());
            ps.setInt(2, shop.isFluctuationEnabled() ? 1 : 0);
            ps.setInt(3, 0);
            ps.setInt(4, shop.getVolatilityMinPercent());
            ps.setInt(5, shop.getVolatilityMaxPercent());
            ps.setInt(6, shop.getPeriodMinutes());
            ps.setInt(7, shop.getPeriodMinutes());
            ps.setString(8, shop.getShopMode().name());
            ps.setString(9, shop.getId());
            ps.executeUpdate();
        }
    }

    public void saveSpawn(ShopSpawn spawn) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO shop_spawns(entity_uuid,shop_id,world,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?,?)")) {
            ps.setString(1, spawn.entityUuid().toString());
            ps.setString(2, spawn.shopId());
            ps.setString(3, spawn.world());
            ps.setDouble(4, spawn.x()); ps.setDouble(5, spawn.y()); ps.setDouble(6, spawn.z());
            ps.setFloat(7, spawn.yaw()); ps.setFloat(8, spawn.pitch());
            ps.executeUpdate();
        }
    }

    public void deleteSpawn(UUID entityId) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM shop_spawns WHERE entity_uuid=?")) {
            ps.setString(1, entityId.toString());
            ps.executeUpdate();
        }
    }
}
