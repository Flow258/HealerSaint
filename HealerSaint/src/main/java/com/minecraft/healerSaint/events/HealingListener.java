package com.minecraft.healerSaint.events;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecraft.healerSaint.HealerSaint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HealingListener implements Listener {
    private final HealerSaint plugin;

    public HealingListener(HealerSaint plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        // Make sure it's the main hand and the entity is a player
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player healer = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        // Check if the healer is a saint
        if (!plugin.getSaintManager().isSaint(healer)) {
            return;
        }

        // Check if the healer is holding the saint's staff
        ItemStack heldItem = healer.getInventory().getItemInMainHand();
        if (heldItem.getType() != Material.STICK || !heldItem.hasItemMeta() ||
                !heldItem.getItemMeta().hasDisplayName() ||
                !heldItem.getItemMeta().getDisplayName().contains("Saint's Staff")) {
            return;
        }

        // Check if healing is on cooldown
        if (plugin.getSaintManager().isHealingOnCooldown(healer)) {
            int remaining = plugin.getSaintManager().getRemainingHealingCooldown(healer);
            healer.sendMessage(ChatColor.RED + "Healing Hand is on cooldown! " + remaining + " seconds remaining.");
            return;
        }

        // Calculate amount to heal (5 hearts = 10 health points)
        double healAmount = 10.0;

        // Apply healing
        double newHealth = Math.min(target.getHealth() + healAmount, target.getMaxHealth());
        target.setHealth(newHealth);

        // Set cooldown
        plugin.getSaintManager().setHealingCooldown(healer);

        // Visual and sound effects
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

        // Messages
        healer.sendMessage(ChatColor.GREEN + "You have healed " + target.getName() + " for 5 hearts!");
        target.sendMessage(ChatColor.GREEN + "You have been healed for 5 hearts by " +
                ChatColor.GOLD + "Saint " + healer.getName() + ChatColor.GREEN + "!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deadPlayer = event.getEntity();
        UUID currentSaintUUID = plugin.getSaintManager().getCurrentSaint();

        // Check if there is a current saint
        if (currentSaintUUID == null) {
            return;
        }

        Player saint = Bukkit.getPlayer(currentSaintUUID);

        // Check if saint is online and nearby
        if (saint == null || !saint.isOnline() || saint.equals(deadPlayer) ||
                !saint.getWorld().equals(deadPlayer.getWorld()) ||
                saint.getLocation().distance(deadPlayer.getLocation()) > plugin.getConfig().getDouble("resurrection_range", 10.0)) {
            return;
        }

        // Check if resurrection is on cooldown for this player
        if (plugin.getSaintManager().isResurrectionOnCooldown(deadPlayer.getUniqueId())) {
            return;
        }

        // Roll for resurrection chance
        if (!plugin.getSaintManager().rollForResurrection()) {
            return;
        }

        // Save the items (only half of them)
        List<ItemStack> savedItems = new ArrayList<>();
        List<ItemStack> drops = event.getDrops();

        int itemsToSave = Math.max(1, drops.size() / 2);
        for (int i = 0; i < itemsToSave && !drops.isEmpty(); i++) {
            savedItems.add(drops.remove(0));
        }

        // Set resurrection on cooldown
        plugin.getSaintManager().setResurrectionCooldown(deadPlayer.getUniqueId());

        // Schedule resurrection after respawn
        new BukkitRunnable() {
            @Override
            public void run() {
                // Make sure player is still online
                if (deadPlayer.isOnline()) {
                    // Return saved items to player
                    for (ItemStack item : savedItems) {
                        if (item != null && item.getType() != Material.AIR) {
                            deadPlayer.getInventory().addItem(item);
                        }
                    }

                    // Visual and sound effects
                    deadPlayer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deadPlayer.getLocation(), 50, 0.5, 1, 0.5, 0.1);
                    deadPlayer.playSound(deadPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

                    // Messages
                    saint.sendMessage(ChatColor.GOLD + "Your holy power has resurrected " +
                            deadPlayer.getName() + " with half of their items!");
                    deadPlayer.sendMessage(ChatColor.GOLD + "You have been resurrected by " +
                            ChatColor.GOLD + "Saint " + saint.getName() +
                            ChatColor.GOLD + " with half of your items!");

                    // Broadcast message
                    String message = ChatColor.GOLD + "✝ " + ChatColor.YELLOW + "Saint " +
                            saint.getName() + " has performed a miracle, resurrecting " +
                            deadPlayer.getName() + "! ✝";
                    Bukkit.broadcastMessage(message);
                }
            }
        }.runTaskLater(plugin, 5L); // Run 5 ticks after death (respawn happens at this point)
    }
}