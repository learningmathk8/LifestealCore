package com.lifesteal.core.api;

import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.manager.DataManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LifestealAPI {
    private final LifestealCore plugin;

    public LifestealAPI(LifestealCore plugin) {
        this.plugin = plugin;
    }

    public int getCombatTimeLeft(@NotNull Player player) {
        return plugin.getCombatListener().getCombatTimeLeft(player);
    }

    public boolean isInCombat(@NotNull Player player) {
        return plugin.getCombatListener().isInCombat(player);
    }

    public int getCurrentHearts(@NotNull Player player) {
        DataManager.PlayerData data = plugin.getDataManager().getData(player);
        return data.currentHearts;
    }

    public int getMaxHearts(@NotNull Player player) {
        DataManager.PlayerData data = plugin.getDataManager().getData(player);
        return data.maxHearts;
    }

    public int getPrestigeLevel(@NotNull Player player) {
        DataManager.PlayerData data = plugin.getDataManager().getData(player);
        return data.prestigeLevel;
    }
}
