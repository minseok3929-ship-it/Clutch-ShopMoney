package com.clutch.shopmoney.service;

import com.clutch.shopmoney.model.Shop;
import com.clutch.shopmoney.model.ShopItem;
import com.clutch.shopmoney.model.ShopSpawn;
import com.clutch.shopmoney.repository.ShopRepository;
import com.clutch.shopmoney.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;

import static com.clutch.shopmoney.util.ItemUtil.SHOP_ID_KEY;

public class ShopService {
    private final JavaPlugin plugin;
    private final ShopRepository repository;
    private final Map<String, Shop> shops = new LinkedHashMap<>();
    private final Map<UUID, String> spawnedShopMap = new HashMap<>();

    public ShopService(JavaPlugin plugin, ShopRepository repository) {this.plugin = plugin; this.repository = repository;}

    public void load() throws SQLException { shops.clear(); shops.putAll(repository.loadAll()); }
    public Collection<Shop> all() { return shops.values(); }
    public Shop get(String id) { return shops.get(id); }

    public void create(String id) throws SQLException { repository.createShop(id, id); load(); }

    public boolean delete(String id) throws SQLException {
        boolean ok = repository.deleteShop(id);
        if (ok) {
            List<UUID> remove = new ArrayList<>();
            for (Map.Entry<UUID, String> e : spawnedShopMap.entrySet()) if (e.getValue().equals(id)) remove.add(e.getKey());
            for (UUID uuid : remove) {
                Entity entity = Bukkit.getEntity(uuid);
                if (entity != null) entity.remove();
                spawnedShopMap.remove(uuid);
            }
            load();
        }
        return ok;
    }

    public Villager spawn(String id, Location location) throws SQLException {
        Shop shop = shops.get(id);
        if (shop == null || location.getWorld() == null) return null;
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false); villager.setInvulnerable(true); villager.setCustomNameVisible(true); villager.setCustomName("§e[상점] " + shop.getName());
        villager.getPersistentDataContainer().set(new NamespacedKey(plugin, SHOP_ID_KEY), PersistentDataType.STRING, id);
        spawnedShopMap.put(villager.getUniqueId(), id);
        repository.saveSpawn(new ShopSpawn(id, villager.getUniqueId(), location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
        return villager;
    }

    public String findShopId(Entity entity) {
        if (entity == null) return null;
        return entity.getPersistentDataContainer().get(new NamespacedKey(plugin, SHOP_ID_KEY), PersistentDataType.STRING);
    }

    public String despawnLookingShop(Player player) throws SQLException {
        List<Entity> nearby = player.getNearbyEntities(6, 6, 6);
        Entity best = null;
        double bestDot = 0.92D;
        for (Entity entity : nearby) {
            if (!(entity instanceof Villager)) continue;
            String id = findShopId(entity);
            if (id == null) continue;
            Location eye = player.getEyeLocation();
            var direction = eye.getDirection().normalize();
            var to = entity.getLocation().toVector().subtract(eye.toVector()).normalize();
            double dot = direction.dot(to);
            if (dot > bestDot) {
                bestDot = dot;
                best = entity;
            }
        }
        if (best == null) return null;
        String shopId = findShopId(best);
        if (shopId == null) return null;
        UUID uuid = best.getUniqueId();
        best.remove();
        spawnedShopMap.remove(uuid);
        repository.deleteSpawn(uuid);
        Shop shop = shops.get(shopId);
        return shop == null ? shopId : shop.getName();
    }

    public void restoreFromWorldScan() {
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                String id = findShopId(villager);
                if (id != null && shops.containsKey(id)) {
                    spawnedShopMap.put(villager.getUniqueId(), id);
                    villager.setAI(false); villager.setInvulnerable(true); villager.setCustomNameVisible(true); villager.setCustomName("§e[상점] " + shops.get(id).getName());
                }
            }
        }
    }

    public int adjustedPercent(Shop shop) {
        return shop.isFluctuationEnabled() ? Math.max(-50, Math.min(80, shop.getFluctuationPercent())) : 0;
    }

    public long currentBuy(Shop shop, ShopItem item) {
        return Math.max(1, Math.round(item.getBuyPrice() * (1 + adjustedPercent(shop) / 100.0)));
    }

    public long currentSell(Shop shop, ShopItem item) {
        return Math.max(1, Math.round(item.getSellPrice() * (1 + adjustedPercent(shop) / 100.0)));
    }

    public boolean isSameTradeItem(ItemStack a, ItemStack b) {
        return ItemUtil.isSameForSelling(a, b);
    }

    public ShopRepository repository() { return repository; }
}
