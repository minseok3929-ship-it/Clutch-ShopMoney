package com.clutch.shopmoney.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class MoneyTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) out.add("보내기");
        if (args.length == 2 && "보내기".equals(args[0])) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        return out;
    }
}
