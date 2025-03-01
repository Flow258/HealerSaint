package com.minecraft.healerSaint.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.minecraft.healerSaint.HealerSaint;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class SaintManager {
    private final HealerSaint plugin;
    private final File dataFile;
    private FileConfiguration data;

    private UUID currentSaint;
    private Map<UUID, Long> healingCooldowns = new HashMap<>();
    private Map<UUID, Long> resurrectionCooldowns = new HashMap<>();

    private Random random = new Random();

    public SaintManager(HealerSaint plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");

        // Create data file if it doesn't exist
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml!");
                e.printStackTrace();
            }
        }

        // Load data
        this.data = YamlConfiguration.loadConfiguration(dataFile);

        // Load current saint
        if (data.contains("current_saint")) {
            String uuidString = data.getString("current_saint");
            if (uuidString != null && !uuidString.isEmpty()) {
                this.currentSaint = UUID.fromString(uuidString);
            }
        }
    }

    /**
     * Checks if a player should become a Healer Saint
     * @param player The player to check
     * @return true if the player becomes a Saint, false otherwise
     */
    public boolean checkNewSaint(Player player) {
        // If there's already a saint and config doesn't allow multiple, return false
        if (currentSaint != null && !plugin.getConfig().getBoolean("allow_multiple_saints", false)) {
            return false;
        }

        // If player has exempt permission, return false
        if (player.hasPermission("saint.exempt")) {
            return false;
        }

        // Get chance from config (default to 1,000,000)
        int chance = plugin.getConfig().getInt("saint_chance", 1000000);

        // Check if this player has joined before
        if (!player.hasPlayedBefore() || !data.contains("players." + player.getUniqueId().toString())) {
            // Player is new, roll the dice
            int roll = random.nextInt(chance);

            // If roll is 0 (1 in chance), player becomes a saint
            if (roll == 0) {
                setSaint(player);
                return true;
            }

            // Record that this player has joined
            data.set("players." + player.getUniqueId().toString(), System.currentTimeMillis());
            saveData();
        }

        return false;
    }

    /**
     * Sets a player as the Healer Saint
     * @param player The player to set as Saint
     */
    public void setSaint(Player player) {
        // Save the new saint's UUID
        currentSaint = player.getUniqueId();
        data.set("current_saint", currentSaint.toString());
        saveData();

        // Give saint kit and effects
        plugin.giveSaintKit(player);
        plugin.applySaintEffects(player);

        // Announce the new saint
        plugin.announceSaint(player);
    }

    /**
     * Removes a player's Saint status
     * @param player The player to remove Saint status from
     */
    public void removeSaint(Player player) {
        // Only proceed if this player is actually the current saint
        if (player.getUniqueId().equals(currentSaint)) {
            // Remove saint effects
            plugin.removeSaintEffects(player);

            // Update data
            currentSaint = null;
            data.set("current_saint", null);
            saveData();

            // Inform the player
            player.sendMessage(ChatColor.RED + "You are no longer the Healer Saint.");
        }
    }

    /**
     * Check if a player is the Healer Saint
     * @param player The player to check
     * @return true if the player is the Healer Saint, false otherwise
     */
    public boolean isSaint(Player player) {
        return player.getUniqueId().equals(currentSaint);
    }

    /**
     * Get the current Healer Saint
     * @return The UUID of the current Saint, or null if none exists
     */
    public UUID getCurrentSaint() {
        return currentSaint;
    }

    /**
     * Check if the Healing Hand ability is on cooldown for a Saint
     * @param player The Healer Saint to check
     * @return true if on cooldown, false otherwise
     */
    public boolean isHealingOnCooldown(Player player) {
        if (!healingCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }

        long cooldownTime = plugin.getConfig().getInt("healing_hand_cooldown", 30) * 1000L;
        long lastUsed = healingCooldowns.get(player.getUniqueId());

        return System.currentTimeMillis() - lastUsed < cooldownTime;
    }

    /**
     * Set the Healing Hand ability on cooldown for a Saint
     * @param player The Healer Saint to set on cooldown
     */
    public void setHealingCooldown(Player player) {
        healingCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Get remaining cooldown time for Healing Hand in seconds
     * @param player The Healer Saint to check
     * @return Remaining cooldown in seconds
     */
    public int getRemainingHealingCooldown(Player player) {
        if (!healingCooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }

        long cooldownTime = plugin.getConfig().getInt("healing_hand_cooldown", 30) * 1000L;
        long lastUsed = healingCooldowns.get(player.getUniqueId());
        long remaining = cooldownTime - (System.currentTimeMillis() - lastUsed);

        return Math.max(0, (int) (remaining / 1000));
    }

    /**
     * Check if resurrection is on cooldown for a specific player
     * @param targetUUID The UUID of the player who died
     * @return true if on cooldown, false otherwise
     */
    public boolean isResurrectionOnCooldown(UUID targetUUID) {
        if (!resurrectionCooldowns.containsKey(targetUUID)) {
            return false;
        }

        long cooldownHours = plugin.getConfig().getInt("resurrection_cooldown", 2);
        long cooldownTime = cooldownHours * 60 * 60 * 1000L; // Convert hours to milliseconds
        long lastResurrection = resurrectionCooldowns.get(targetUUID);

        return System.currentTimeMillis() - lastResurrection < cooldownTime;
    }

    /**
     * Set resurrection on cooldown for a specific player
     * @param targetUUID The UUID of the player who was resurrected
     */
    public void setResurrectionCooldown(UUID targetUUID) {
        resurrectionCooldowns.put(targetUUID, System.currentTimeMillis());
    }

    /**
     * Check if a resurrection should succeed based on chance
     * @return true if resurrection succeeds, false otherwise
     */
    public boolean rollForResurrection() {
        int chance = plugin.getConfig().getInt("resurrection_chance", 10);
        int roll = random.nextInt(100);

        return roll < chance;
    }

    /**
     * Save data to file
     */
    public void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml!");
            e.printStackTrace();
        }
    }

    /**
     * Reload data from file
     */
    public void reloadData() {
        this.data = YamlConfiguration.loadConfiguration(dataFile);

        // Reload current saint
        if (data.contains("current_saint")) {
            String uuidString = data.getString("current_saint");
            if (uuidString != null && !uuidString.isEmpty()) {
                this.currentSaint = UUID.fromString(uuidString);
            } else {
                this.currentSaint = null;
            }
        }
    }
}