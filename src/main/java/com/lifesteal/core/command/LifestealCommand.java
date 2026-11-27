package com.lifesteal.core.command;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.LifestealCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LifestealCommand implements CommandExecutor {
    private final LifestealCore plugin;

    public LifestealCommand(LifestealCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (command.getName().equalsIgnoreCase("lifesteal")) {
            plugin.getMenus().openMainMenu(player);
        } else if (command.getName().equalsIgnoreCase("lsconfig")) {
            if (!player.isOp() && !player.hasPermission("lifesteal.admin") && !player.hasPermission("group.owner") && !player.hasPermission("group.admin")) {
                player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>No permission."));
                return true;
            }
            plugin.getMenus().openLSConfig(player);
        } else if (command.getName().equalsIgnoreCase("clconfig")) {
            if (!player.isOp() && !player.hasPermission("lifesteal.admin") && !player.hasPermission("group.owner") && !player.hasPermission("group.admin")) {
                player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>No permission."));
                return true;
            }
            plugin.getMenus().openCLConfig(player);
        }
        return true;
    }
}
