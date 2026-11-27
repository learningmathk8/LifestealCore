package com.lifesteal.core.listener;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.manager.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatListener implements Listener {
    private final LifestealCore plugin;
    private final Map<UUID, Long> combatTagged = new HashMap<>();
    private BukkitTask tickerTask;

    public CombatListener(LifestealCore plugin) {
        this.plugin = plugin;
        startSubtitleTicker();
    }

    public void tagPlayer(Player player) {
        long expire = System.currentTimeMillis() + (plugin.getConfigManager().combatTimerSeconds * 1000L);
        combatTagged.put(player.getUniqueId(), expire);
    }

    public boolean isInCombat(Player player) {
        UUID uuid = player.getUniqueId();
        return combatTagged.containsKey(uuid) && combatTagged.get(uuid) > System.currentTimeMillis();
    }

    public int getCombatTimeLeft(Player player) {
        UUID uuid = player.getUniqueId();
        if (!combatTagged.containsKey(uuid)) return 0;
        long diff = combatTagged.get(uuid) - System.currentTimeMillis();
        return (int) Math.max(0, diff / 1000L);
    }

    private void startSubtitleTicker() {
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Map.Entry<UUID, Long> entry : new HashMap<>(combatTagged).entrySet()) {
                if (entry.getValue() <= now) {
                    combatTagged.remove(entry.getKey());
                } else {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    if (p != null && p.isOnline()) {
                        int timeLeft = (int) Math.max(0, (entry.getValue() - now) / 1000L);
                        String msgTemplate = plugin.getConfigManager().combatSubtitleMsg;
                        String formatted = msgTemplate.replace("{time}", String.valueOf(timeLeft));
                        p.sendActionBar(LifestealCore.fmt(formatted));
                    }
                }
            }
        }, 0L, 20L);
    }

    public void stopTicker() {
        if (tickerTask != null) tickerTask.cancel();
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player damager) {
            tagPlayer(victim);
            tagPlayer(damager);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (isInCombat(player)) {
            if (plugin.getConfigManager().punishOnLogout) {
                if (plugin.getConfigManager().heartPenaltyOnLog) {
                    DataManager.PlayerData data = plugin.getDataManager().getData(player);
                    data.currentHearts = Math.max(0, data.currentHearts - 1);
                    plugin.getDataManager().savePlayer(player);
                }
                if (plugin.getConfigManager().itemsDropOnLogout) {
                    for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() != org.bukkit.Material.AIR) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }
                    }
                    for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
                        if (item != null && item.getType() != org.bukkit.Material.AIR) {
                            player.getWorld().dropItemNaturally(player.getLocation(), item);
                        }
                    }
                    player.getInventory().clear();
                }
                player.setHealth(0.0);
            }
        }
        combatTagged.remove(uuid);
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfigManager().blockCommands && isInCombat(player)) {
            String msg = event.getMessage().toLowerCase();
            if (msg.startsWith("/spawn") || msg.startsWith("/home") || msg.startsWith("/tp") || msg.startsWith("/withdraw")) {
                event.setCancelled(true);
                player.sendMessage(LifestealCore.fmt("<red><bold>✖ <gray>You cannot use this command while in combat!"));
            }
        }
    }
}
