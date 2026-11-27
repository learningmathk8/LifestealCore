package com.lifesteal.core.manager;

import com.lifesteal.core.LifestealCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {
    private final LifestealCore plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final NamespacedKey heartKey;

    public static class PlayerData {
        public String lastKnownName;
        public int currentHearts;
        public int maxHearts;
        public int prestigeLevel;
        public String peakDate;
        public boolean depositToInventory = false; // false = Health Bar, true = Inventory
        public boolean consumeFromInventory = true; // true = Inventory, false = Health Bar
        public long lastKillTime = 0;

        public PlayerData(String name, int current, int max, int prestige, String peak, boolean depInv, boolean conInv) {
            this.lastKnownName = name;
            this.currentHearts = current;
            this.maxHearts = max;
            this.prestigeLevel = prestige;
            this.peakDate = peak;
            this.depositToInventory = depInv;
            this.consumeFromInventory = conInv;
        }
    }

    public DataManager(LifestealCore plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.heartKey = new NamespacedKey(plugin, "lifesteal_heart_item");
    }

    public PlayerData getData(Player player) {
        PlayerData data = cache.computeIfAbsent(player.getUniqueId(), uuid -> loadFromFile(player));
        data.lastKnownName = player.getName();
        return data;
    }

    private PlayerData loadFromFile(Player player) {
        File file = new File(dataFolder, player.getUniqueId().toString() + ".yml");
        int defMax = plugin.getConfigManager().defaultMaxHearts;
        if (!file.exists()) {
            String date = java.time.LocalDate.now().toString();
            return new PlayerData(player.getName(), defMax, defMax, 0, date, false, true);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String name = cfg.getString("player-name", player.getName());
        int current = cfg.getInt("current-hearts", defMax);
        int max = cfg.getInt("max-hearts", defMax);
        int prestige = cfg.getInt("prestige-level", 0);
        String peak = cfg.getString("peak-date", java.time.LocalDate.now().toString());
        boolean depInv = cfg.getBoolean("deposit-to-inventory", false);
        boolean conInv = cfg.getBoolean("consume-from-inventory", true);
        return new PlayerData(name, current, max, prestige, peak, depInv, conInv);
    }

    public void syncHeartsToPlayer(Player player) {
        PlayerData data = getData(player);
        if (data.currentHearts > data.maxHearts) {
            data.currentHearts = data.maxHearts;
        }
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            double targetMaxHealth = Math.max(2.0, data.maxHearts * 2.0);
            attr.setBaseValue(targetMaxHealth);
        }
        double targetHealth = Math.min(player.getMaxHealth(), Math.max(1.0, data.currentHearts * 2.0));
        if (player.getHealth() > targetHealth) {
            player.setHealth(targetHealth);
        }
    }

    public ItemStack createPhysicalHeart(int amount) {
        ItemStack item = new ItemStack(Material.RED_DYE, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(LifestealCore.fmt("<red><bold>❤ Lifesteal Heart ❤"));
            meta.lore(List.of(
                    LifestealCore.fmt("<gray>A pulsing container of life force."),
                    LifestealCore.fmt("<yellow>▸ Right-Click to consume & gain +1 Heart!"),
                    LifestealCore.fmt("<dark_gray>Can also be stored or traded.")
            ));
            meta.getPersistentDataContainer().set(heartKey, PersistentDataType.BYTE, (byte) 1);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isPhysicalHeart(ItemStack item) {
        if (item == null || item.getType() != Material.RED_DYE || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(heartKey, PersistentDataType.BYTE);
    }

    public void giveOrDropHeart(Player player, int amount) {
        PlayerData data = getData(player);
        // If player preference is deposit to inventory OR player has reached absolute max hearts, give physical item
        if (data.depositToInventory || data.maxHearts >= plugin.getConfigManager().absoluteMaxHearts) {
            ItemStack hearts = createPhysicalHeart(amount);
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(hearts);
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            player.sendMessage(LifestealCore.fmt("<green><bold>✔ <gray>Received <red>+" + amount + " Physical Heart(s) <gray>in your inventory/drops!"));
        } else {
            // Directly added to health bar up to absolute max hearts
            int allowedAdd = Math.min(amount, plugin.getConfigManager().absoluteMaxHearts - data.maxHearts);
            int overflowAmount = amount - allowedAdd;
            
            if (allowedAdd > 0) {
                data.maxHearts += allowedAdd;
                data.currentHearts += allowedAdd;
                syncHeartsToPlayer(player);
                savePlayer(player);
                player.sendMessage(LifestealCore.fmt("<green><bold>❤ +<red>" + allowedAdd + " Max & Current Heart(s) <gray>added directly to your health bar!"));
            }
            if (overflowAmount > 0) {
                ItemStack hearts = createPhysicalHeart(overflowAmount);
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(hearts);
                for (ItemStack drop : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.sendMessage(LifestealCore.fmt("<yellow>⚠ Absolute max heart cap reached! Excess hearts dropped into your inventory/ground."));
            }
        }
    }

    public void savePlayer(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return;
        File file = new File(dataFolder, player.getUniqueId().toString() + ".yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        cfg.set("player-name", player.getName());
        cfg.set("current-hearts", data.currentHearts);
        cfg.set("max-hearts", data.maxHearts);
        cfg.set("prestige-level", data.prestigeLevel);
        cfg.set("peak-date", data.peakDate);
        cfg.set("deposit-to-inventory", data.depositToInventory);
        cfg.set("consume-from-inventory", data.consumeFromInventory);
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAllSync() {
        for (UUID uuid : cache.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) savePlayer(p);
        }
    }
}
