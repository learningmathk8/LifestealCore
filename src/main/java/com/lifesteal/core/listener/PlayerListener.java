package com.lifesteal.core.listener;

import com.lifesteal.core.LifestealCore;
import com.lifesteal.core.manager.DataManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    private final LifestealCore plugin;

    public PlayerListener(LifestealCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getDataManager().getData(player);
        plugin.getDataManager().syncHeartsToPlayer(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getDataManager().savePlayer(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (plugin.getDataManager().isPhysicalHeart(item)) {
                event.setCancelled(true);
                item.setAmount(item.getAmount() - 1);
                
                DataManager.PlayerData data = plugin.getDataManager().getData(player);
                if (data.consumeFromInventory && data.maxHearts < plugin.getConfigManager().absoluteMaxHearts) {
                    data.maxHearts++;
                    data.currentHearts++;
                    plugin.getDataManager().syncHeartsToPlayer(player);
                    plugin.getDataManager().savePlayer(player);
                    player.sendMessage(LifestealCore.fmt("<green><bold>❤ +1 Heart consumed into your health bar!"));
                } else {
                    // Give heart via preference
                    plugin.getDataManager().giveOrDropHeart(player, 1);
                }
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        DataManager.PlayerData vData = plugin.getDataManager().getData(victim);

        // Victim loses 1 heart
        vData.currentHearts = Math.max(0, vData.currentHearts - 1);
        plugin.getDataManager().syncHeartsToPlayer(victim);
        plugin.getDataManager().savePlayer(victim);

        // Killer gains 1 heart (either directly or as physical item drop/deposit)
        if (killer != null && plugin.getConfigManager().pvpHeartSteal && killer != victim) {
            plugin.getDataManager().giveOrDropHeart(killer, 1);
            killer.sendMessage(LifestealCore.fmt("<green><bold>❤ +1 HEART! <gray>You defeated <red>" + victim.getName() + "<gray>!"));
        }

        if (vData.currentHearts <= 0) {
            victim.banPlayer("You ran out of Lifesteal hearts!");
        }
    }
}
