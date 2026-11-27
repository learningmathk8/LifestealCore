package com.lifesteal.core.command;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.manager.DataManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WithdrawCommand implements CommandExecutor {
    private final LifestealCore plugin;

    public WithdrawCommand(LifestealCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length < 1) {
            player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Usage: <yellow>/withdraw <amount>"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>Invalid number format."));
            return true;
        }

        if (amount < 1) {
            player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>You must withdraw at least 1 heart."));
            return true;
        }

        DataManager.PlayerData data = plugin.getDataManager().getData(player);

        if (data.currentHearts <= amount || data.maxHearts <= amount) {
            player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>You cannot withdraw that many hearts! You must keep at least 1 heart remaining."));
            return true;
        }

        data.maxHearts -= amount;
        data.currentHearts -= amount;
        plugin.getDataManager().syncHeartsToPlayer(player);
        plugin.getDataManager().savePlayer(player);

        org.bukkit.inventory.ItemStack heartsItem = plugin.getDataManager().createPhysicalHeart(amount);
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> overflow = player.getInventory().addItem(heartsItem);
        for (org.bukkit.inventory.ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        player.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Successfully withdrew <red>" + amount + " heart(s) <gray>into physical items!"));
        return true;
    }
}
