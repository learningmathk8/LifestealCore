package com.lifesteal.core.manager;

import com.lifesteal.core.LifestealCore;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final LifestealCore plugin;

    public int defaultMaxHearts = 15;
    public int absoluteMaxHearts = 30;
    public int prestigeBaseCost = 15;
    public int prestigeCostMultiplier = 10;
    public boolean pvpHeartSteal = true;
    public long killCooldownSeconds = 300;
    public boolean prestigeAnimation = true;

    public int combatTimerSeconds = 25;
    public boolean punishOnLogout = true;
    public boolean heartPenaltyOnLog = true;
    public boolean itemsDropOnLogout = true;
    public boolean blockCommands = true;
    public boolean blockFlight = true;
    public String combatSubtitleMsg = "<red><bold>COMBAT TAGGED: <yellow>{time}s";

    public ConfigManager(LifestealCore plugin) {
        this.plugin = plugin;
        loadConfigValues();
    }

    public void loadConfigValues() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        c.addDefault("lifesteal.default-max-hearts", 15);
        c.addDefault("lifesteal.absolute-max-hearts", 30);
        c.addDefault("lifesteal.prestige-base-cost", 15);
        c.addDefault("lifesteal.prestige-cost-multiplier", 10);
        c.addDefault("lifesteal.pvp-heart-steal", true);
        c.addDefault("lifesteal.kill-cooldown-seconds", 300);
        c.addDefault("lifesteal.prestige-animation", true);

        c.addDefault("combatlog.timer-seconds", 25);
        c.addDefault("combatlog.punish-on-logout", true);
        c.addDefault("combatlog.heart-penalty", true);
        c.addDefault("combatlog.items-drop-on-logout", true);
        c.addDefault("combatlog.block-commands", true);
        c.addDefault("combatlog.block-flight", true);
        c.addDefault("combatlog.subtitle-message", "<red><bold>COMBAT TAGGED: <yellow>{time}s");

        c.options().copyDefaults(true);
        plugin.saveConfig();

        defaultMaxHearts = c.getInt("lifesteal.default-max-hearts", 15);
        absoluteMaxHearts = c.getInt("lifesteal.absolute-max-hearts", 30);
        prestigeBaseCost = c.getInt("lifesteal.prestige-base-cost", 15);
        prestigeCostMultiplier = c.getInt("lifesteal.prestige-cost-multiplier", 10);
        pvpHeartSteal = c.getBoolean("lifesteal.pvp-heart-steal", true);
        killCooldownSeconds = c.getLong("lifesteal.kill-cooldown-seconds", 300);
        prestigeAnimation = c.getBoolean("lifesteal.prestige-animation", true);

        combatTimerSeconds = c.getInt("combatlog.timer-seconds", 25);
        punishOnLogout = c.getBoolean("combatlog.punish-on-logout", true);
        heartPenaltyOnLog = c.getBoolean("combatlog.heart-penalty", true);
        itemsDropOnLogout = c.getBoolean("combatlog.items-drop-on-logout", true);
        blockCommands = c.getBoolean("combatlog.block-commands", true);
        blockFlight = c.getBoolean("combatlog.block-flight", true);
        combatSubtitleMsg = c.getString("combatlog.subtitle-message", "<red><bold>COMBAT TAGGED: <yellow>{time}s");
    }

    public void saveConfigValues() {
        FileConfiguration c = plugin.getConfig();
        c.set("lifesteal.default-max-hearts", defaultMaxHearts);
        c.set("lifesteal.absolute-max-hearts", absoluteMaxHearts);
        c.set("lifesteal.prestige-base-cost", prestigeBaseCost);
        c.set("lifesteal.prestige-cost-multiplier", prestigeCostMultiplier);
        c.set("lifesteal.pvp-heart-steal", pvpHeartSteal);
        c.set("lifesteal.kill-cooldown-seconds", killCooldownSeconds);
        c.set("lifesteal.prestige-animation", prestigeAnimation);

        c.set("combatlog.timer-seconds", combatTimerSeconds);
        c.set("combatlog.punish-on-logout", punishOnLogout);
        c.set("combatlog.heart-penalty", heartPenaltyOnLog);
        c.set("combatlog.items-drop-on-logout", itemsDropOnLogout);
        c.set("combatlog.block-commands", blockCommands);
        c.set("combatlog.block-flight", blockFlight);
        c.set("combatlog.subtitle-message", combatSubtitleMsg);
        plugin.saveConfig();
    }
}
