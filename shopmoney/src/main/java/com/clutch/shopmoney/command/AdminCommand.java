package com.clutch.shopmoney.command;

import com.clutch.shopmoney.gui.GuiFactory;
import com.clutch.shopmoney.model.Account;
import com.clutch.shopmoney.service.MoneyService;
import com.clutch.shopmoney.service.ShopService;
import com.clutch.shopmoney.util.ItemUtil;
import com.clutch.shopmoney.util.MessageUtil;
import com.clutch.shopmoney.util.NumberUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private static final Pattern SHOP_NAME_PATTERN = Pattern.compile("^[0-9A-Za-z가-힣_]+$");

    private final JavaPlugin plugin;
    private final MoneyService moneyService;
    private final ShopService shopService;
    private final GuiFactory guiFactory;
    private final MessageUtil messageUtil;

    public AdminCommand(JavaPlugin plugin, MoneyService moneyService, ShopService shopService, GuiFactory guiFactory, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.moneyService = moneyService;
        this.shopService = shopService;
        this.guiFactory = guiFactory;
        this.messageUtil = messageUtil;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("clutch.money.admin") && !player.hasPermission("clutch.shop.admin")) {
            messageUtil.send(player, "§c권한이 없습니다.");
            return true;
        }
        if (args.length < 1) {
            messageUtil.send(player, "§f사용법: /t <돈|상점> ...");
            return true;
        }

        if ("돈".equals(args[0])) {
            return handleMoney(player, args);
        }
        if ("상점".equals(args[0])) {
            return handleShop(player, args);
        }

        messageUtil.send(player, "§f사용법: /t <돈|상점> ...");
        return true;
    }

    private boolean handleMoney(Player player, String[] args) {
        if (!player.hasPermission("clutch.money.admin")) {
            messageUtil.send(player, "§c권한이 없습니다.");
            return true;
        }
        try {
            if (args.length >= 3 && "생성".equals(args[1])) {
                Long amount = NumberUtil.parsePositiveLong(args[2]);
                if (amount == null) {
                    messageUtil.send(player, "§c금액 오류");
                    return true;
                }
                player.getInventory().addItem(ItemUtil.createCash(plugin, amount, plugin.getConfig().getInt("cash.customModelData", 10001)));
                messageUtil.send(player, "§f현금 아이템 생성 완료");
                return true;
            }
            if (args.length >= 2 && "순위".equals(args[1])) {
                List<Account> top = moneyService.top10();
                messageUtil.send(player, "§f[돈 순위 TOP10]");
                int rank = 1;
                for (Account account : top) {
                    messageUtil.send(player, "§e" + rank++ + ". §f" + account.lastKnownName() + " - " + account.balance());
                }
                return true;
            }
        } catch (SQLException e) {
            messageUtil.send(player, "§c관리자 명령 처리 실패");
            plugin.getLogger().severe("[Clutch] Failed money admin command");
            e.printStackTrace();
            return true;
        }
        messageUtil.send(player, "§f사용법: /t 돈 <생성|순위>");
        return true;
    }

    private boolean handleShop(Player player, String[] args) {
        if (!player.hasPermission("clutch.shop.admin")) {
            messageUtil.send(player, "§c권한이 없습니다.");
            return true;
        }
        if (args.length < 2) {
            messageUtil.send(player, "§f사용법: /t 상점 <생성|삭제|스폰|편집|디스폰>");
            return true;
        }

        try {
            if ("생성".equals(args[1])) {
                if (args.length < 3) {
                    messageUtil.send(player, "§f사용법: /t 상점 생성 <이름>");
                    return true;
                }
                if (args.length > 3) {
                    messageUtil.send(player, "§c상점 이름에 공백은 사용할 수 없습니다.");
                    return true;
                }

                String shopName = args[2].trim();
                if (!SHOP_NAME_PATTERN.matcher(shopName).matches()) {
                    messageUtil.send(player, "§c상점 이름은 한글/영문/숫자/밑줄만 가능합니다.");
                    return true;
                }
                if (shopService.existsShopName(shopName)) {
                    messageUtil.send(player, "§c이미 존재하는 상점 이름입니다.");
                    return true;
                }

                plugin.getLogger().info("[Clutch] Creating shop: " + shopName);
                try {
                    shopService.create(shopName);
                    messageUtil.send(player, "§f상점 생성 완료: §e" + shopName);
                } catch (SQLException e) {
                    messageUtil.send(player, "§c상점 생성 실패");
                    plugin.getLogger().severe("[Clutch] Failed to create shop: " + shopName);
                    e.printStackTrace();
                }
                return true;
            }

            if ("삭제".equals(args[1])) {
                if (args.length < 3) {
                    messageUtil.send(player, "§f사용법: /t 상점 삭제 <이름>");
                    return true;
                }
                boolean ok = shopService.delete(args[2]);
                messageUtil.send(player, ok ? "§f상점 삭제 완료" : "§c상점이 없습니다.");
                return true;
            }

            if ("스폰".equals(args[1])) {
                player.openInventory(guiFactory.spawnTickets(shopService.all()));
                return true;
            }
            if ("편집".equals(args[1])) {
                player.openInventory(guiFactory.editList(shopService.all()));
                return true;
            }
            if ("디스폰".equals(args[1])) {
                String name = shopService.despawnLookingShop(player);
                if (name == null) {
                    messageUtil.send(player, "§c바라보는 상점 주민을 찾지 못했습니다.");
                } else {
                    messageUtil.send(player, "§f상점을 디스폰했습니다: §e" + name);
                }
                return true;
            }
        } catch (SQLException e) {
            messageUtil.send(player, "§c관리자 명령 처리 실패");
            plugin.getLogger().severe("[Clutch] Failed shop admin command");
            e.printStackTrace();
            return true;
        }

        messageUtil.send(player, "§f사용법: /t 상점 <생성|삭제|스폰|편집|디스폰>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) { out.add("돈"); out.add("상점"); }
        else if (args.length == 2 && "돈".equals(args[0])) { out.add("생성"); out.add("순위"); }
        else if (args.length == 2 && "상점".equals(args[0])) { out.add("생성"); out.add("삭제"); out.add("스폰"); out.add("편집"); out.add("디스폰"); }
        return out;
    }
}
