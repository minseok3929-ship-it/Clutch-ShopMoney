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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.clutch.shopmoney.util.ItemUtil.SHOP_ID_KEY;

public class ShopService {
    public static final List<Integer> INNER_SLOTS = List.of(10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43);
    private final JavaPlugin plugin;
    private final ShopRepository repository;
    private final Map<String, Shop> shops = new LinkedHashMap<>();
    private final Map<UUID, String> spawnedShopMap = new HashMap<>();
    private final Map<String, Long> nextFluctuationAt = new HashMap<>();

    public ShopService(JavaPlugin plugin, ShopRepository repository) {this.plugin = plugin; this.repository = repository;}

    public void load() throws SQLException {
        shops.clear();
        shops.putAll(repository.loadAll());
        for (Shop shop : shops.values()) {
            for (ShopItem item : shop.getItems()) {
                if (item.getItem() != null) ItemUtil.applyShopItemId(plugin, item.getItem(), shopItemId(item.getSlot()));
                normalizeItemPrices(item);
            }
        }
    }
    public Collection<Shop> all() { return shops.values(); }
    public Shop get(String id) { return shops.get(id); }

    public boolean existsShopName(String name) {
        return shops.values().stream().anyMatch(s -> s.getName().equalsIgnoreCase(name));
    }

    public void create(String id) throws SQLException {
        repository.createShop(id, id);
        load();
    }

    public void startFluctuationTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickFluctuation, 20L * 60, 20L * 60);
    }

    private void tickFluctuation() {
        long now = System.currentTimeMillis();
        for (Shop shop : shops.values()) {
            if (!shop.isFluctuationEnabled()) continue;
            long next = nextFluctuationAt.getOrDefault(shop.getId(), 0L);
            if (now < next) continue;
            applyFluctuation(shop);
            nextFluctuationAt.put(shop.getId(), now + Math.max(1, shop.getPeriodMinutes()) * 60_000L);
        }
    }

    public void applyFluctuation(Shop shop) {
        int min = Math.max(-50, shop.getVolatilityMinPercent());
        int max = Math.min(80, shop.getVolatilityMaxPercent());
        if (min > max) { int t = min; min = max; max = t; }
        int randomPercent = ThreadLocalRandom.current().nextInt(min, max + 1);
        double multiplier = 1 + (randomPercent / 100.0);

        for (ShopItem item : shop.getItems()) {
            item.setPreviousBuyPrice(item.getCurrentBuyPrice() <= 0 ? item.getBuyPrice() : item.getCurrentBuyPrice());
            item.setPreviousSellPrice(item.getCurrentSellPrice() <= 0 ? item.getSellPrice() : item.getCurrentSellPrice());
            long computedBuy = Math.max(1, Math.round(item.getBuyPrice() * multiplier));
            long computedSell = Math.max(1, Math.round(item.getSellPrice() * multiplier));
            item.setCurrentBuyPrice(applyGuard(item.getBuyPrice(), computedBuy));
            item.setCurrentSellPrice(applyGuard(item.getSellPrice(), computedSell));
        }
        try {
            repository.replaceItems(shop.getId(), shop.getItems());
        } catch (SQLException ignored) {}
    }

    private long applyGuard(long base, long calculated) {
        ConfigurationSection guard = plugin.getConfig().getConfigurationSection("shop.priceGuard");
        if (guard == null || !guard.getBoolean("enabled", true)) return calculated;
        double minM = guard.getDouble("minMultiplier", 0.5);
        double maxM = guard.getDouble("maxMultiplier", 1.8);
        long min = Math.max(1, Math.round(base * minM));
        long max = Math.max(min, Math.round(base * maxM));
        if (calculated >= min && calculated <= max) return calculated;
        String mode = guard.getString("mode", "RESET_TO_BASE");
        if ("CLAMP".equalsIgnoreCase(mode)) {
            return Math.max(min, Math.min(max, calculated));
        }
        return base;
    }

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
            if (dot > bestDot) { bestDot = dot; best = entity; }
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


    public ItemStack buildGiveItem(ShopItem item, int amount) {
        ItemStack give = item.getItem().clone();
        give.setAmount(amount);
        return give;
    }

    public ItemStack buildDisplayItem(Shop shop, ShopItem item) {
        ItemStack display = item.getItem().clone();
        var meta = display.getItemMeta();
        if (meta != null) {
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7이전 구매가: §f" + item.getPreviousBuyPrice());
            lore.add("§7현재 구매가: §f" + currentBuy(shop, item));
            lore.add("§7이전 판매가: §f" + item.getPreviousSellPrice());
            lore.add("§7현재 판매가: §f" + currentSell(shop, item));
            switch (shop.getShopMode()) {
                case BUY_ONLY -> {
                    lore.add("§e좌클릭=1개 구매, Shift+좌클릭=64개 구매");
                    lore.add("§c판매 불가 상점");
                }
                case SELL_ONLY -> {
                    lore.add("§e우클릭=1개 판매, Shift+우클릭=전부 판매");
                    lore.add("§c구매 불가 상점");
                }
                default -> {
                    lore.add("§e좌클릭=1개 구매, Shift+좌클릭=64개 구매");
                    lore.add("§e우클릭=1개 판매, Shift+우클릭=전부 판매");
                }
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
        }
        return display;
    }

    public long currentBuy(Shop shop, ShopItem item) { return Math.max(1, item.getCurrentBuyPrice() <= 0 ? item.getBuyPrice() : item.getCurrentBuyPrice()); }
    public long currentSell(Shop shop, ShopItem item) { return Math.max(1, item.getCurrentSellPrice() <= 0 ? item.getSellPrice() : item.getCurrentSellPrice()); }
    public boolean isSameTradeItem(ShopItem item, ItemStack candidate) { return ItemUtil.isSameForSelling(plugin, item.getItem(), candidate, shopItemId(item.getSlot())); }

    public void normalizeItemPrices(ShopItem item) {
        item.setCurrentBuyPrice(applyGuard(item.getBuyPrice(), item.getCurrentBuyPrice() <= 0 ? item.getBuyPrice() : item.getCurrentBuyPrice()));
        item.setCurrentSellPrice(applyGuard(item.getSellPrice(), item.getCurrentSellPrice() <= 0 ? item.getSellPrice() : item.getCurrentSellPrice()));
    }

    public void saveEditPage(Shop shop, int page, Inventory top) {
        int start = page * INNER_SLOTS.size();
        Map<Integer, ShopItem> bySlot = new HashMap<>();
        for (ShopItem item : shop.getItems()) bySlot.put(item.getSlot(), item);

        for (int i = 0; i < INNER_SLOTS.size(); i++) {
            int viewSlot = INNER_SLOTS.get(i);
            int absoluteSlot = start + i;
            ItemStack stack = top.getItem(viewSlot);
            if (stack == null || stack.getType().isAir()) {
                bySlot.remove(absoluteSlot);
                continue;
            }
            ShopItem existing = bySlot.get(absoluteSlot);
            if (existing == null) {
                ItemStack base = one(stack);
                ItemUtil.applyShopItemId(plugin, base, shopItemId(absoluteSlot));
                ShopItem created = new ShopItem(0, base, 1, 1, 1, 1, 1, 1, absoluteSlot);
                normalizeItemPrices(created);
                bySlot.put(absoluteSlot, created);
            } else {
                ItemStack base = one(stack);
                ItemUtil.applyShopItemId(plugin, base, shopItemId(absoluteSlot));
                existing.setItem(base);
                bySlot.put(absoluteSlot, existing);
            }
        }

        List<ShopItem> updated = new ArrayList<>(bySlot.values());
        updated.sort(Comparator.comparingInt(ShopItem::getSlot));
        shop.getItems().clear();
        shop.getItems().addAll(updated);
        try { repository.replaceItems(shop.getId(), shop.getItems()); } catch (SQLException ignored) {}
    }

    private ItemStack one(ItemStack in) { ItemStack c = in.clone(); c.setAmount(1); return c; }

    public String shopItemId(int absoluteSlot) {
        return "shop-item-" + absoluteSlot;
    }

    public int totalPages(Shop shop) {
        int max = shop.getItems().stream().mapToInt(ShopItem::getSlot).max().orElse(-1);
        return Math.max(1, (int) Math.ceil((max + 1) / (double) INNER_SLOTS.size()));
    }

    public ShopRepository repository() { return repository; }
}
