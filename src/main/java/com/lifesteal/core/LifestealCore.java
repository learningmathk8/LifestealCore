package com.lifesteal.core;

import com.lifesteal.core.api.LifestealAPI;
import com.lifesteal.core.command.LSAdminCommand;
import com.lifesteal.core.command.LifestealCommand;
import com.lifesteal.core.command.WithdrawCommand;
import com.lifesteal.core.listener.CombatListener;
import com.lifesteal.core.listener.PlayerListener;
import com.lifesteal.core.manager.ConfigManager;
import com.lifesteal.core.manager.DataManager;
import com.lifesteal.core.menu.Menus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public final class LifestealCore extends JavaPlugin {

    private static LifestealCore instance;
    private ConfigManager configManager;
    private DataManager dataManager;
    private Menus menus;
    private CombatListener combatListener;
    private LifestealAPI api;
    public static final MiniMessage MM = MiniMessage.miniMessage();

    public static Component fmt(String miniMessageText) {
        return MM.deserialize(miniMessageText).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.dataManager = new DataManager(this);
        this.menus = new Menus(this);
        this.combatListener = new CombatListener(this);
        this.api = new LifestealAPI(this);

        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(combatListener, this);

        LifestealCommand cmd = new LifestealCommand(this);
        getCommand("lifesteal").setExecutor(cmd);
        getCommand("lsconfig").setExecutor(cmd);
        getCommand("clconfig").setExecutor(cmd);

        LSAdminCommand adminCmd = new LSAdminCommand(this);
        getCommand("lsadmin").setExecutor(adminCmd);
        getCommand("lsadmin").setTabCompleter(adminCmd);

        WithdrawCommand withdrawCmd = new WithdrawCommand(this);
        getCommand("withdraw").setExecutor(withdrawCmd);

        getLogger().info("LifestealCore has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllSync();
        }
        getLogger().info("LifestealCore disabled.");
    }

    public static LifestealCore getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public Menus getMenus() { return menus; }
    public CombatListener getCombatListener() { return combatListener; }
    public LifestealAPI getApi() { return api; }
}