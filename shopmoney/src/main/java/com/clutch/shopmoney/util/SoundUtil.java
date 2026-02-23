package com.clutch.shopmoney.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {
    private SoundUtil() {
    }

    public static void error(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    public static void success(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
