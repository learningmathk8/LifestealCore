package com.lifesteal.core.command;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.manager.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LSAdminCommand implements CommandExecutor, TabCompleter {
    private final LifestealCore plugin;

    public LSAdminCommand(LifestealCore plugin) {
        this.plugin = plugin;
    }

    private boolean hasAdminPermission(CommandSender sender) {
        if (sender.isOp()) return true;
        if (sender.hasPermission("lifesteal.admin")) return true;
        // Check LuckPerms groups if available
        if (sender instanceof Player player) {
            if (player.hasPermission("group.owner") || player.hasPermission("group.admin")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPermission(sender)) {
            sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>You do not have permission to use Lifesteal admin commands."));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.getMenus().openAdminPanel(player);
            } else {
                sender.sendMessage("Usage: /lsadmin <sethearts|setmaxhearts|setprestige|giveheart|inspect|reset|reload>");
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("reload")) {
            plugin.getConfigManager().loadConfigValues();
            sender.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>LifestealCore config.yml reloaded successfully!"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Please specify a target player!"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Player <yellow>" + args[1] + " <gray>must be online!"));
            return true;
        }

        DataManager.PlayerData data = plugin.getDataManager().getData(target);

        switch (sub) {
            case "inspect" -> {
                sender.sendMessage(LifestealCore.fmt("<dark_gray>──────── <gold><bold>" + target.getName() + "'s Lifesteal Stats</bold> <dark_gray>────────"));
                sender.sendMessage(LifestealCore.fmt("<gray>Current Hearts: <red><bold>" + data.currentHearts + " ❤"));
                sender.sendMessage(LifestealCore.fmt("<gray>Max Hearts: <red><bold>" + data.maxHearts + " ❤"));
                sender.sendMessage(LifestealCore.fmt("<gray>Prestige Level: <gold><bold>★ " + data.prestigeLevel));
                sender.sendMessage(LifestealCore.fmt("<gray>Peak Recorded: <yellow>" + data.peakDate));
            }

            case "reset" -> {
                int defMax = plugin.getConfigManager().defaultMaxHearts;
                data.maxHearts = defMax;
                data.currentHearts = defMax;
                data.prestigeLevel = 0;
                data.peakDate = java.time.LocalDate.now().toString();
                plugin.getDataManager().syncHeartsToPlayer(target);
                plugin.getDataManager().savePlayer(target);
                sender.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Reset <yellow>" + target.getName() + "<gray>'s hearts and prestige to default (" + defMax + " hearts)."));
            }

            case "sethearts" -> {
                if (args.length < 3) {
                    sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Usage: <yellow>/lsadmin sethearts <player> <amount>"));
                    return true;
                }
                int amount = parsePositiveInt(sender, args[2], 1);
                if (amount < 1) return true;

                if (amount > data.maxHearts) {
                    sender.sendMessage(LifestealCore.fmt(
                            "<red><bold>✖ <red>Please try to set this player's max hearts before setting their current hearts to <yellow>" + amount +
                            " <gray>(Current Max Hearts: <red>" + data.maxHearts + "<gray>)."
                    ));
                    return true;
                }

                data.currentHearts = amount;
                plugin.getDataManager().syncHeartsToPlayer(target);
                plugin.getDataManager().savePlayer(target);
                sender.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Set <yellow>" + target.getName() + "<gray>'s current hearts to <red><bold>" + amount + " ❤<gray>."));
            }

            case "setmaxhearts" -> {
                if (args.length < 3) {
                    sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Usage: <yellow>/lsadmin setmaxhearts <player> <amount>"));
                    return true;
                }
                int amount = parsePositiveInt(sender, args[2], 1);
                if (amount < 1) return true;

                data.maxHearts = amount;
                if (data.currentHearts > data.maxHearts) {
                    data.currentHearts = data.maxHearts;
                }
                data.peakDate = java.time.LocalDate.now().toString();
                plugin.getDataManager().syncHeartsToPlayer(target);
                plugin.getDataManager().savePlayer(target);
                sender.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Set <yellow>" + target.getName() + "<gray>'s max hearts to <red><bold>" + amount + " ❤<gray>."));
            }

            case "setprestige" -> {
                if (args.length < 3) {
                    sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Usage: <yellow>/lsadmin setprestige <player> <level>"));
                    return true;
                }
                int level = parsePositiveInt(sender, args[2], 0);
                if (level < 0) return true;

                data.prestigeLevel = level;
                plugin.getDataManager().savePlayer(target);
                sender.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Set <yellow>" + target.getName() + "<gray>'s prestige level to <gold><bold>★ " + level + "<gray>."));
            }

            case "giveheart" -> {
                if (args.length < 3) {
                    sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Usage: <yellow>/lsadmin giveheart <player> <quantity>"));
                    return true;
                }
                int qty = parsePositiveInt(sender, args[2], 1);
                if (qty < 1) return true;

                plugin.getDataManager().giveOrDropHeart(target, qty);
                sender.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Gave <red>" + qty + " physical heart(s) <gray>to <yellow>" + target.getName() + "<gray>."));
            }

            default -> {
                if (sender instanceof Player player) {
                    plugin.getMenus().openAdminPanel(player);
                } else {
                    sender.sendMessage("Unknown subcommand. Use /lsadmin for help.");
                }
            }
        }
        return true;
    }

    private int parsePositiveInt(CommandSender sender, String raw, int min) {
        try {
            int val = Integer.parseInt(raw);
            if (val < min) {
                sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Number must be at least <yellow>" + min + "<gray>."));
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            sender.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Invalid number: <yellow>" + raw));
            return -1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!hasAdminPermission(sender)) return List.of();
        if (args.length == 1) {
            List<String> subs = List.of("sethearts", "setmaxhearts", "setprestige", "giveheart", "inspect", "reset", "reload");
            List<String> out = new ArrayList<>();
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
            return out;
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return null; // online players
        }
        if (args.length == 3 && List.of("sethearts", "setmaxhearts", "setprestige", "giveheart").contains(args[0].toLowerCase())) {
            return List.of("1", "5", "10", "15", "20");
        }
        return List.of();
    }
}
