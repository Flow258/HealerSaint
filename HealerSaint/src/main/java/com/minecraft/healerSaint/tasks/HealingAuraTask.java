package com.minecraft.healerSaint.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.minecraft.healerSaint.HealerSaint;

import java.util.UUID;

public class HealingAuraTask extends BukkitRunnable {
    private final HealerSaint plugin;

    public HealingAuraTask(HealerSaint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        UUID saintUUID = plugin.getSaintManager().getCurrentSaint();

        // If there's no saint, do nothing
        if (saintUUID == null) {
            return;
        }

        // Get the saint player
        Player saint = Bukkit.getPlayer(saintUUID);

        // If saint is offline, do nothing
        if (saint == null || !saint.isOnline()) {
            return;
        }

        // Get healing radius and amount from config
        double radius = plugin.getConfig().getDouble("healing_radius", 5.0);
        double healingPerSecond = plugin.getConfig().getDouble("healing_per_second", 0.5);

        // Get nearby entities
        for (Entity entity : saint.getNearbyEntities(radius, radius, radius)) {
            // Only heal players, not mobs
            if (entity instanceof Player) {
                Player target = (Player) entity;

                // Skip dead or full health players
                if (target.isDead() || target.getHealth() >= target.getMaxHealth()) {
                    continue;
                }

                // Apply healing
                double newHealth = Math.min(target.getHealth() + healingPerSecond, target.getMaxHealth());
                target.setHealth(newHealth);



                // Only show particles if player was actually healed
                if (newHealth > target.getHealth()) {
                    // Visual effect (small hearts)
                    target.getWorld().spawnParticle(
                            Particle.HEART,

                            target.getLocation().add(0, 1.5, 0),
                            1, 0.2, 0.2, 0.2, 0
                    );
                }
            }
        }

        // Visual effect for the saint (golden particles)
        saint.getWorld().spawnParticle(
                Particle.END_ROD,
                saint.getLocation().add(0, 1, 0),
                3, 0.5, 0.5, 0.5, 0.02
        );
    }
}