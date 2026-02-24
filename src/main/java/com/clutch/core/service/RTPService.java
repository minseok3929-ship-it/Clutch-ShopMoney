package com.clutch.core.service;

import com.clutch.core.ClutchCorePlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RTPService {
    private final ClutchCorePlugin plugin;
    private final Map<UUID, Long> cooldownMap = new ConcurrentHashMap<>();

    public RTPService(ClutchCorePlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOnCooldown(Player player) {
        long cooldownMillis = plugin.getConfig().getLong("rtp.cooldownSeconds", 180L) * 1000L;
        long expiresAt = cooldownMap.getOrDefault(player.getUniqueId(), 0L);
        return System.currentTimeMillis() < expiresAt && cooldownMillis > 0;
    }

    public long getRemainingMillis(Player player) {
        return Math.max(0L, cooldownMap.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis());
    }

    public Location findSafeLocation() {
        String worldName = plugin.getConfig().getString("worlds.wild", "Wild");
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }

        int minRadius = plugin.getConfig().getInt("rtp.minRadius", 500);
        int maxRadius = plugin.getConfig().getInt("rtp.maxRadius", 8000);
        int tries = plugin.getConfig().getInt("rtp.maxAttempts", 30);

        for (int i = 0; i < tries; i++) {
            int[] coords = randomCoords(minRadius, maxRadius);
            int x = coords[0];
            int z = coords[1];

            int y = world.getHighestBlockYAt(x, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            Block ground = world.getBlockAt(x, y - 1, z);

            if (isSafe(feet, head, ground)) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        return null;
    }

    public void applyCooldown(Player player) {
        long cooldownMillis = plugin.getConfig().getLong("rtp.cooldownSeconds", 180L) * 1000L;
        cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMillis);
    }

    private int[] randomCoords(int minRadius, int maxRadius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0, Math.PI * 2);
        int radius = random.nextInt(Math.min(minRadius, maxRadius), Math.max(minRadius, maxRadius) + 1);
        int x = (int) Math.round(Math.cos(angle) * radius);
        int z = (int) Math.round(Math.sin(angle) * radius);
        return new int[]{x, z};
    }

    private boolean isSafe(Block feet, Block head, Block ground) {
        if (!ground.getType().isSolid()) {
            return false;
        }

        if (!isPassableForPlayer(feet) || !isPassableForPlayer(head)) {
            return false;
        }

        Material groundType = ground.getType();
        Material feetType = feet.getType();

        if (groundType == Material.LAVA || groundType == Material.WATER) {
            return false;
        }
        if (feetType == Material.LAVA || feetType == Material.WATER) {
            return false;
        }

        return !groundType.name().contains("LAVA")
                && !groundType.name().contains("WATER")
                && !feetType.name().contains("LAVA")
                && !feetType.name().contains("WATER");
    }

    private boolean isPassableForPlayer(Block block) {
        return block.isEmpty() || block.isPassable();
    }
}
