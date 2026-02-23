package com.clutch.shopmoney.repository;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private final String url;

    public Database(JavaPlugin plugin) {
        File dbFile = new File(plugin.getDataFolder(), "clutch.db");
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    public void init() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS accounts(uuid TEXT PRIMARY KEY, name TEXT NOT NULL, balance INTEGER NOT NULL DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS shops(id TEXT PRIMARY KEY, name TEXT NOT NULL, fluctuation_enabled INTEGER NOT NULL, fluctuation_percent INTEGER NOT NULL, period_minutes INTEGER NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS shop_items(id INTEGER PRIMARY KEY AUTOINCREMENT, shop_id TEXT NOT NULL, item_base64 TEXT NOT NULL, buy_price INTEGER NOT NULL, sell_price INTEGER NOT NULL, previous_buy INTEGER NOT NULL, previous_sell INTEGER NOT NULL, slot INTEGER NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS shop_spawns(entity_uuid TEXT PRIMARY KEY, shop_id TEXT NOT NULL, world TEXT NOT NULL, x REAL, y REAL, z REAL, yaw REAL, pitch REAL)");
        }
    }
}
