package com.clutch.shopmoney.repository;

import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.model.ShopSpawn;
import com.clutch.shopmoney.util.SerializationUtil;

import java.sql.*;
import java.util.*;

public class ShopRepository {
    private final Database database;

    public ShopRepository(Database database) {
        this.database = database;
    }

    public void createShop(String id, String name) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO shops(id,name,fluctuation_enabled,fluctuation_percent,period_minutes) VALUES(?,?,?,?,?)")) {
            ps.setString(1, id);
            ps.setString(2, name);
            ps.setInt(3, 1);
            ps.setInt(4, 0);
            ps.setInt(5, 10);
            ps.executeUpdate();
        }
    }

    public boolean deleteShop(String id) throws SQLException {
        try (Connection conn = database.getConnection()) {
            try (PreparedStatement item = conn.prepareStatement("DELETE FROM shop_items WHERE shop_id=?")) {
                item.setString(1, id); item.executeUpdate();
            }
            try (PreparedStatement spawn = conn.prepareStatement("DELETE FROM shop_spawns WHERE shop_id=?")) {
                spawn.setString(1, id); spawn.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM shops WHERE id=?")) {
                ps.setString(1, id);
                return ps.executeUpdate() > 0;
            }
        }
    }

    public Optional<Shop> find(String id) throws SQLException { return Optional.ofNullable(loadAll().get(id)); }

    public Map<String, Shop> loadAll() throws SQLException {
        Map<String, Shop> map = new LinkedHashMap<>();
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM shops"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Shop s = new Shop(rs.getString("id"), rs.getString("name"), rs.getInt("fluctuation_enabled") == 1, rs.getInt("fluctuation_percent"), rs.getInt("period_minutes"));
                map.put(s.getId(), s);
            }
        }
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM shop_items ORDER BY slot ASC"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Shop shop = map.get(rs.getString("shop_id"));
                if (shop == null) continue;
                ShopItem item = new ShopItem(rs.getInt("id"), SerializationUtil.itemFromBase64(rs.getString("item_base64")), rs.getLong("buy_price"), rs.getLong("sell_price"), rs.getLong("previous_buy"), rs.getLong("previous_sell"), rs.getInt("slot"));
                shop.getItems().add(item);
            }
        }
        return map;
    }

    public void addShopItem(String shopId, ShopItem item) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO shop_items(shop_id,item_base64,buy_price,sell_price,previous_buy,previous_sell,slot) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, shopId);
            ps.setString(2, SerializationUtil.itemToBase64(item.getItem()));
            ps.setLong(3, item.getBuyPrice());
            ps.setLong(4, item.getSellPrice());
            ps.setLong(5, item.getPreviousBuyPrice());
            ps.setLong(6, item.getPreviousSellPrice());
            ps.setInt(7, item.getSlot());
            ps.executeUpdate();
        }
    }

    public void updateShopItem(ShopItem item) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE shop_items SET item_base64=?, buy_price=?, sell_price=?, previous_buy=?, previous_sell=?, slot=? WHERE id=?")) {
            ps.setString(1, SerializationUtil.itemToBase64(item.getItem()));
            ps.setLong(2, item.getBuyPrice());
            ps.setLong(3, item.getSellPrice());
            ps.setLong(4, item.getPreviousBuyPrice());
            ps.setLong(5, item.getPreviousSellPrice());
            ps.setInt(6, item.getSlot());
            ps.setInt(7, item.getId());
            ps.executeUpdate();
        }
    }

    public void deleteShopItem(int itemId) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM shop_items WHERE id=?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        }
    }

    public void updateShopMeta(Shop shop) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE shops SET name=?, fluctuation_enabled=?, fluctuation_percent=?, period_minutes=? WHERE id=?")) {
            ps.setString(1, shop.getName());
            ps.setInt(2, shop.isFluctuationEnabled() ? 1 : 0);
            ps.setInt(3, shop.getFluctuationPercent());
            ps.setInt(4, shop.getPeriodMinutes());
            ps.setString(5, shop.getId());
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
