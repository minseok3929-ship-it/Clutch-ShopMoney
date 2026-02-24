package com.clutch.shopmoney.util;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageUtil {
    private final String prefix;

    public MessageUtil(JavaPlugin plugin) {
        this.prefix = plugin.getConfig().getString("message.prefix", "§8[CLUTCH] ");
    }

    public String format(String message) {
        return prefix + message;
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(format(message));
    }
}
