package com.lifesteal.core.menu;

import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.manager.ConfigManager;
import com.lifesteal.core.manager.DataManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class Menus implements Listener {
    private final LifestealCore plugin;
    private final Map<UUID, Integer> leaderboardPages = new HashMap<>();

    public enum MenuType {
        MAIN, PRESTIGE_CONFIRM, SETTINGS, LS_CONFIG, CL_CONFIG, LEADERBOARD, ADMIN_PANEL
    }

    public static class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private Inventory inventory;

        public MenuHolder(MenuType type) {
            this.type = type;
        }

        public MenuType getType() {
            return type;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    public Menus(LifestealCore plugin) {
        this.plugin = plugin;
    }

    private Inventory createStyledInventory(MenuType type, int rows, String titleMiniMessage, Material borderMat, Material fillMat) {
        MenuHolder holder = new MenuHolder(type);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, LifestealCore.fmt(titleMiniMessage));
        holder.setInventory(inv);

        ItemStack border = makePane(borderMat);
        ItemStack filler = makePane(fillMat);

        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, border);
            } else {
                inv.setItem(i, filler);
            }
        }
        return inv;
    }

    private ItemStack makePane(Material mat) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.setHideTooltip(true);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createItem(Material mat, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LifestealCore.fmt(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(LifestealCore.fmt(line));
            }
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openMainMenu(Player player) {
        Inventory inv = createStyledInventory(
                MenuType.MAIN, 6,
                "<gradient:#ff2a2a:#ff8c00><bold>✦ LIFESTEAL CORE MANAGEMENT ✦</bold></gradient>",
                Material.RED_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );

        DataManager.PlayerData data = plugin.getDataManager().getData(player);
        ConfigManager cfg = plugin.getConfigManager();
        int prestigeCost = cfg.prestigeBaseCost + (cfg.prestigeCostMultiplier * data.prestigeLevel);

        inv.setItem(11, createItem(Material.RED_DYE, "<red><bold>Current Active Hearts", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Your current health pool in combat.",
                "<gray>Current: <red><bold>" + data.currentHearts + " ❤",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Defeat enemies or consume",
                "<yellow>  physical hearts to restore."
        )));

        inv.setItem(13, createItem(Material.NETHER_STAR, "<gold><bold>Maximum Heart Cap", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Your current maximum health limit.",
                "<gray>Max Cap: <red><bold>" + data.maxHearts + " ❤",
                "<gray>Server Ceiling: <yellow>" + cfg.absoluteMaxHearts + " ❤",
                "<dark_gray>────────────────────────",
                "<gray>Increase by prestiging or",
                "<gray>absorbing heart containers."
        )));

        inv.setItem(15, createItem(Material.EXPERIENCE_BOTTLE, "<gradient:#ffd700:#ff8c00><bold>⚡ Prestige & Heart Cost</bold></gradient>", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Current Prestige Tier: <gold><bold>★ " + data.prestigeLevel,
                "<gray>Sacrifice Cost: <red><bold>" + prestigeCost + " Hearts",
                "<gray>Reward: <green><bold>+1 Max Heart Cap",
                "<dark_gray>────────────────────────",
                data.maxHearts >= cfg.absoluteMaxHearts ? "<red><bold>✖ MAXIMUM CAP REACHED" : "<yellow>▸ Click to Initiate Prestige!"
        )));

        inv.setItem(29, createItem(Material.BOOK, "<gradient:#facc15:#f59e0b><bold>🏆 Heart Leaderboard</bold></gradient>", List.of(
                "<dark_gray>────────────────────────",
                "<gray>View top players with the highest",
                "<gray>max hearts and peak achievements.",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Click to Open Leaderboard"
        )));

        inv.setItem(31, createItem(Material.COMPARATOR, "<aqua><bold>⚙ Player Preferences", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Customize where your hearts go on death,",
                "<gray>inventory vs health bar toggles,",
                "<gray>and withdrawal preferences.",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Click to Configure Settings"
        )));

        inv.setItem(33, createItem(Material.GOLD_INGOT, "<yellow><bold>/withdraw Command", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Convert your health hearts into",
                "<gray>physical heart items (/withdraw).",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Type /withdraw <amount> in chat"
        )));

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        player.openInventory(inv);
    }

    public void openPrestigeConfirm(Player player) {
        Inventory inv = createStyledInventory(
                MenuType.PRESTIGE_CONFIRM, 3,
                "<dark_red><bold>Confirm Prestige Upgrade</bold></dark_red>",
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );

        DataManager.PlayerData data = plugin.getDataManager().getData(player);
        ConfigManager cfg = plugin.getConfigManager();
        int cost = cfg.prestigeBaseCost + (cfg.prestigeCostMultiplier * data.prestigeLevel);

        inv.setItem(11, createItem(Material.EMERALD_BLOCK, "<green><bold>✔ CONFIRM PRESTIGE", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Sacrifice <red>" + cost + " Hearts",
                "<gray>Advance to Prestige <gold><bold>" + (data.prestigeLevel + 1),
                "<dark_gray>────────────────────────",
                "<yellow>▸ Click to Confirm & Apply"
        )));

        inv.setItem(15, createItem(Material.RED_CONCRETE, "<red><bold>✖ CANCEL & GO BACK", List.of(
                "<dark_gray>────────────────────────",
                "<gray>Abort prestige and return",
                "<gray>to the main menu.",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Click to Cancel"
        )));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f);
        player.openInventory(inv);
    }

    public void openPlayerSettings(Player player) {
        Inventory inv = createStyledInventory(
                MenuType.SETTINGS, 3,
                "<aqua><bold>Player Heart Preferences</bold></aqua>",
                Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );

        DataManager.PlayerData data = plugin.getDataManager().getData(player);

        String depText = data.depositToInventory ? "<green>INVENTORY / DROPS" : "<gold>HEALTH BAR DIRECT";
        inv.setItem(11, createItem(Material.CHEST, "<yellow><bold>Heart Drop Destination: " + depText, List.of(
                "<dark_gray>────────────────────────",
                "<gray>Choose where earned hearts go:",
                "<gray>• <gold>Health Bar: <gray>Added directly to health.",
                "<gray>• <green>Inventory: <gray>Given as physical red dye",
                "<gray>  items (or dropped if full).",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Click to Toggle"
        )));

        String conText = data.consumeFromInventory ? "<green>INVENTORY ITEMS" : "<gold>HEALTH BAR";
        inv.setItem(15, createItem(Material.RED_DYE, "<light_purple><bold>Heart Consumption Source: " + conText, List.of(
                "<dark_gray>────────────────────────",
                "<gray>Choose where hearts are taken from",
                "<gray>when consuming physical hearts.",
                "<dark_gray>────────────────────────",
                "<yellow>▸ Click to Toggle"
        )));

        player.openInventory(inv);
    }

    public void openLeaderboard(Player player, int page) {
        leaderboardPages.put(player.getUniqueId(), page);

        Inventory inv = createStyledInventory(
                MenuType.LEADERBOARD, 6,
                "<gradient:#facc15:#ea580c><bold>🏆 HEART LEADERBOARD (Page " + page + ") 🏆</bold></gradient>",
                Material.YELLOW_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );

        File folder = new File(plugin.getDataFolder(), "userdata");
        File[] files = folder.listFiles();
        List<File> fileList = new ArrayList<>();
        if (files != null) {
            for (File f : files) if (f.getName().endsWith(".yml")) fileList.add(f);
        }

        fileList.sort((f1, f2) -> {
            FileConfiguration c1 = YamlConfiguration.loadConfiguration(f1);
            FileConfiguration c2 = YamlConfiguration.loadConfiguration(f2);
            return Integer.compare(c2.getInt("max-hearts", 15), c1.getInt("max-hearts", 15));
        });

        int pageSize = 28;
        int totalPlayers = fileList.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalPlayers / pageSize));

        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalPlayers);

        int[] contentSlots = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
        };

        int slotIdx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (slotIdx >= contentSlots.length) break;
            File f = fileList.get(i);
            FileConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            String displayName = cfg.getString("player-name", f.getName().replace(".yml", ""));
            int maxH = cfg.getInt("max-hearts", 15);
            int curH = cfg.getInt("current-hearts", 15);
            int pres = cfg.getInt("prestige-level", 0);
            String peak = cfg.getString("peak-date", "Unknown");

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(displayName));
                meta.displayName(LifestealCore.fmt("<gold><bold>#" + (i + 1) + " <white>" + displayName));
                List<Component> lore = new ArrayList<>();
                lore.add(LifestealCore.fmt("<dark_gray>────────────────────"));
                lore.add(LifestealCore.fmt("<gray>Max Hearts: <red><bold>" + maxH + " ❤"));
                lore.add(LifestealCore.fmt("<gray>Current Hearts: <red>" + curH + " ❤"));
                lore.add(LifestealCore.fmt("<gray>Prestige Tier: <gold><bold>★ " + pres));
                lore.add(LifestealCore.fmt("<gray>Peak Achieved: <yellow>" + peak));
                lore.add(LifestealCore.fmt("<dark_gray>────────────────────"));
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                head.setItemMeta(meta);
            }

            inv.setItem(contentSlots[slotIdx++], head);
        }

        if (page > 1) {
            inv.setItem(48, createItem(Material.ARROW, "<yellow><bold>← Previous Page", List.of("<gray>Go to page " + (page - 1))));
        }
        if (page < totalPages) {
            inv.setItem(50, createItem(Material.ARROW, "<yellow><bold>Next Page →", List.of("<gray>Go to page " + (page + 1))));
        }

        inv.setItem(49, createItem(Material.BARRIER, "<red><bold>← Back to Main Menu", List.of("<gray>Return to management screen")));
        player.openInventory(inv);
    }

    public void openLSConfig(Player player) {
        Inventory inv = createStyledInventory(
                MenuType.LS_CONFIG, 4,
                "<gradient:#ff3333:#aa0000><bold>⚙ Lifesteal Settings</bold></gradient>",
                Material.RED_STAINED_GLASS_PANE,
                Material.GRAY_STAINED_GLASS_PANE
        );
        ConfigManager cfg = plugin.getConfigManager();

        inv.setItem(10, createItem(Material.APPLE, "<red><bold>Default Max Hearts: <white>" + cfg.defaultMaxHearts, List.of("<gray>Starting max heart limit.", "", "<green>▸ Left: +1", "<red>▸ Right: -1")));
        inv.setItem(12, createItem(Material.GOLDEN_APPLE, "<gold><bold>Absolute Max Hearts: <white>" + cfg.absoluteMaxHearts, List.of("<gray>Hard ceiling.", "", "<green>▸ Left: +1", "<red>▸ Right: -1")));
        inv.setItem(14, createItem(Material.EXPERIENCE_BOTTLE, "<yellow><bold>Prestige Base Cost: <white>" + cfg.prestigeBaseCost, List.of("<gray>Formula base.", "", "<green>▸ Left: +1", "<red>▸ Right: -1")));
        inv.setItem(16, createItem(Material.DIAMOND_SWORD, "<aqua><bold>PvP Heart Steal: " + (cfg.pvpHeartSteal ? "<green>ENABLED" : "<red>DISABLED"), List.of("<yellow>▸ Click to Toggle")));
        inv.setItem(22, createItem(Material.TOTEM_OF_UNDYING, "<light_purple><bold>Prestige Totem Effect: " + (cfg.prestigeAnimation ? "<green>ENABLED" : "<red>DISABLED"), List.of("<yellow>▸ Click to Toggle")));
        inv.setItem(31, createItem(Material.BARRIER, "<red><bold>Close Menu", List.of("<gray>Saves automatically.")));

        player.openInventory(inv);
    }

    public void openCLConfig(Player player) {
        Inventory inv = createStyledInventory(
                MenuType.CL_CONFIG, 3,
                "<gradient:#a855f7:#6b21a8><bold>⚔ Combat Logger Settings</bold></gradient>",
                Material.PURPLE_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );
        ConfigManager cfg = plugin.getConfigManager();

        inv.setItem(10, createItem(Material.CLOCK, "<yellow><bold>Combat Timer: <white>" + cfg.combatTimerSeconds + "s", List.of("<green>▸ Left: +5s", "<red>▸ Right: -5s")));
        inv.setItem(12, createItem(Material.IRON_DOOR, "<red><bold>Punish on Logout: " + (cfg.punishOnLogout ? "<green>ENABLED" : "<red>DISABLED"), List.of("<yellow>▸ Click to Toggle")));
        inv.setItem(14, createItem(Material.NETHER_WART, "<dark_red><bold>Heart Penalty on Log: " + (cfg.heartPenaltyOnLog ? "<green>ENABLED" : "<red>DISABLED"), List.of("<yellow>▸ Click to Toggle")));
        inv.setItem(16, createItem(Material.CHEST, "<gold><bold>Items Drop on Logout: " + (cfg.itemsDropOnLogout ? "<green>ENABLED" : "<red>DISABLED"), List.of("<yellow>▸ Click to Toggle")));

        player.openInventory(inv);
    }

    public void openAdminPanel(Player player) {
        Inventory inv = createStyledInventory(
                MenuType.ADMIN_PANEL, 5,
                "<gradient:#ff1e56:#ffac41><bold>🛡 LIFESTEAL ADMIN PANEL 🛡</bold></gradient>",
                Material.ORANGE_STAINED_GLASS_PANE,
                Material.BLACK_STAINED_GLASS_PANE
        );

        inv.setItem(4, createItem(Material.BEACON, "<gradient:#ffd700:#ff6b6b><bold>Admin Command Reference</bold></gradient>", List.of("<yellow>▸ Click any icon to paste syntax")));
        inv.setItem(10, createItem(Material.RED_DYE, "<red><bold>/lsadmin sethearts <player> <amount>", List.of("<yellow>▸ Click to paste")));
        inv.setItem(12, createItem(Material.GOLDEN_APPLE, "<gold><bold>/lsadmin setmaxhearts <player> <amount>", List.of("<yellow>▸ Click to paste")));
        inv.setItem(14, createItem(Material.NETHER_STAR, "<light_purple><bold>/lsadmin giveheart <player> <qty>", List.of("<yellow>▸ Click to paste")));
        inv.setItem(16, createItem(Material.SPYGLASS, "<aqua><bold>/lsadmin inspect <player>", List.of("<yellow>▸ Click to paste")));
        inv.setItem(20, createItem(Material.EXPERIENCE_BOTTLE, "<light_purple><bold>/lsadmin setprestige <player> <level>", List.of("<yellow>▸ Click to paste")));
        inv.setItem(22, createItem(Material.MILK_BUCKET, "<white><bold>/lsadmin reset <player>", List.of("<yellow>▸ Click to paste")));
        inv.setItem(30, createItem(Material.COMPARATOR, "<red><bold>Open Lifesteal Config (/lsconfig)", List.of("<yellow>▸ Click")));
        inv.setItem(31, createItem(Material.REPEATER, "<green><bold>Reload Plugin Config (/lsadmin reload)", List.of("<yellow>▸ Click")));
        inv.setItem(32, createItem(Material.IRON_SWORD, "<light_purple><bold>Open CombatLog Config (/clconfig)", List.of("<yellow>▸ Click")));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getSlot();
        MenuType type = holder.getType();

        if (type == MenuType.MAIN) {
            if (slot == 15) {
                openPrestigeConfirm(player);
            } else if (slot == 29) {
                openLeaderboard(player, 1);
            } else if (slot == 31) {
                openPlayerSettings(player);
            }
        } else if (type == MenuType.PRESTIGE_CONFIRM) {
            if (slot == 11) {
                DataManager.PlayerData data = plugin.getDataManager().getData(player);
                ConfigManager cfg = plugin.getConfigManager();
                int cost = cfg.prestigeBaseCost + (cfg.prestigeCostMultiplier * data.prestigeLevel);

                if (data.maxHearts >= cfg.absoluteMaxHearts) {
                    player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>You have reached the absolute maximum heart cap!"));
                    player.closeInventory();
                    return;
                }
                if (data.currentHearts < cost) {
                    player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>You need <red>" + cost + " Hearts <gray>to prestige!"));
                    player.closeInventory();
                    return;
                }

                data.currentHearts -= cost;
                data.maxHearts += 1;
                data.prestigeLevel += 1;
                data.peakDate = java.time.LocalDate.now().toString();
                plugin.getDataManager().syncHeartsToPlayer(player);
                plugin.getDataManager().savePlayer(player);
                player.closeInventory();

                if (cfg.prestigeAnimation) {
                    player.sendEquipmentChange(player, org.bukkit.inventory.EquipmentSlot.OFF_HAND, new ItemStack(Material.RED_DYE));
                    player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
                    player.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
                }
                player.sendMessage(LifestealCore.fmt("<green><bold>✔ PRESTIGE UP! <gray>Reached <gold>Prestige " + data.prestigeLevel + " <gray>(Max Hearts: <red>" + data.maxHearts + "<gray>)"));
            } else if (slot == 15) {
                openMainMenu(player);
            }
        } else if (type == MenuType.SETTINGS) {
            DataManager.PlayerData data = plugin.getDataManager().getData(player);
            if (slot == 11) {
                data.depositToInventory = !data.depositToInventory;
                plugin.getDataManager().savePlayer(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                openPlayerSettings(player);
            } else if (slot == 15) {
                data.consumeFromInventory = !data.consumeFromInventory;
                plugin.getDataManager().savePlayer(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                openPlayerSettings(player);
            }
        } else if (type == MenuType.LEADERBOARD) {
            int currentPage = leaderboardPages.getOrDefault(player.getUniqueId(), 1);

            File folder = new File(plugin.getDataFolder(), "userdata");
            File[] files = folder.listFiles();
            List<File> fileList = new ArrayList<>();
            if (files != null) for (File f : files) if (f.getName().endsWith(".yml")) fileList.add(f);
            int totalPages = Math.max(1, (int) Math.ceil((double) fileList.size() / 28));

            if (slot == 48) {
                if (currentPage > 1) {
                    openLeaderboard(player, currentPage - 1);
                } else {
                    player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>There is no previous page."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                }
            } else if (slot == 50) {
                if (currentPage < totalPages) {
                    openLeaderboard(player, currentPage + 1);
                } else {
                    player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>There is no next page."));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
                }
            } else if (slot == 49) {
                openMainMenu(player);
            }
        } else if (type == MenuType.LS_CONFIG) {
            ConfigManager cfg = plugin.getConfigManager();
            if (slot == 10) cfg.defaultMaxHearts = Math.max(1, cfg.defaultMaxHearts + (event.isLeftClick() ? 1 : -1));
            else if (slot == 12) cfg.absoluteMaxHearts = Math.max(1, cfg.absoluteMaxHearts + (event.isLeftClick() ? 1 : -1));
            else if (slot == 14) cfg.prestigeBaseCost = Math.max(1, cfg.prestigeBaseCost + (event.isLeftClick() ? 1 : -1));
            else if (slot == 16) cfg.pvpHeartSteal = !cfg.pvpHeartSteal;
            else if (slot == 22) cfg.prestigeAnimation = !cfg.prestigeAnimation;
            else if (slot == 31) { player.closeInventory(); return; }
            else return;

            cfg.saveConfigValues();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.3f);
            openLSConfig(player);
        } else if (type == MenuType.CL_CONFIG) {
            ConfigManager cfg = plugin.getConfigManager();
            if (slot == 10) cfg.combatTimerSeconds = Math.max(5, cfg.combatTimerSeconds + (event.isLeftClick() ? 5 : -5));
            else if (slot == 12) cfg.punishOnLogout = !cfg.punishOnLogout;
            else if (slot == 14) cfg.heartPenaltyOnLog = !cfg.heartPenaltyOnLog;
            else if (slot == 16) cfg.itemsDropOnLogout = !cfg.itemsDropOnLogout;
            else return;

            cfg.saveConfigValues();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.3f);
            openCLConfig(player);
        } else if (type == MenuType.ADMIN_PANEL) {
            if (slot == 10) sendCommandHint(player, "/lsadmin sethearts <player> <amount>");
            else if (slot == 12) sendCommandHint(player, "/lsadmin setmaxhearts <player> <amount>");
            else if (slot == 14) sendCommandHint(player, "/lsadmin giveheart <player> <quantity>");
            else if (slot == 16) sendCommandHint(player, "/lsadmin inspect <player>");
            else if (slot == 20) sendCommandHint(player, "/lsadmin setprestige <player> <level>");
            else if (slot == 22) sendCommandHint(player, "/lsadmin reset <player>");
            else if (slot == 30) openLSConfig(player);
            else if (slot == 31) {
                plugin.getConfigManager().loadConfigValues();
                player.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Configuration reloaded!"));
                player.closeInventory();
            } else if (slot == 32) openCLConfig(player);
        }
    }

    private void sendCommandHint(Player player, String cmdSyntax) {
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.5f);
        player.sendMessage(LifestealCore.fmt("<gold><bold>▸ Command Syntax: <yellow><click:suggest_command:'" + cmdSyntax.split("<")[0] + "'>" + cmdSyntax + "</click> <gray>(Click to auto-fill)"));
    }
}