package com.minecraft.healerSaint;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.minecraft.healerSaint.commands.SaintCommand;
import com.minecraft.healerSaint.events.HealingListener;
import com.minecraft.healerSaint.events.PlayerListener;
import com.minecraft.healerSaint.managers.SaintManager;
import com.minecraft.healerSaint.tasks.HealingAuraTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HealerSaint extends JavaPlugin {
    private SaintManager saintManager;
    private HealingAuraTask healingAuraTask;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize manager
        saintManager = new SaintManager(this);

        // Register events
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new HealingListener(this), this);

        // Register commands
        getCommand("saint").setExecutor(new SaintCommand(this));

        // Start healing aura task
        healingAuraTask = new HealingAuraTask(this);
        healingAuraTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("HealerSaint plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (healingAuraTask != null) {
            healingAuraTask.cancel();
        }
        getLogger().info("HealerSaint plugin has been disabled!");
    }

    public SaintManager getSaintManager() {
        return saintManager;
    }

    /**
     * Creates the Saint's Staff item
     * @return The customized staff item
     */
    public ItemStack createSaintStaff() {
        ItemStack staff = new ItemStack(Material.STICK, 1);
        ItemMeta meta = staff.getItemMeta();

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Saint's Staff");

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "A blessed staff with healing powers");
        lore.add(ChatColor.YELLOW + "Right-click on a player to heal them");
        lore.add(ChatColor.GRAY + "Cooldown: " + getConfig().getInt("healing_hand_cooldown") + " seconds");

        meta.setLore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 9999999, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);


        staff.setItemMeta(meta);
        return staff;
    }

    /**
     * Gives the Healer Saint kit to a player
     * @param player The player to receive the kit
     */
    public void giveSaintKit(Player player) {
        // Give golden armor with enchantments
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);

        for (ItemStack item : new ItemStack[]{helmet, chestplate, leggings, boots}) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.BLAST_PROTECTION, 10, true);
            meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 10, true);
            meta.addEnchant(Enchantment.PROTECTION, 10, true);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + "Healer's Robe");
            lore.add(ChatColor.GOLD + "Blessed by the GODS");
            lore.add(ChatColor.GOLD + "Armor of the Saint");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        player.getInventory().addItem(helmet, chestplate, leggings, boots);

        // Give saint's staff
        player.getInventory().addItem(createSaintStaff());

        // Give golden apples
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2000, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 2000, 1, false, false));
    }

    /**
     * Broadcasts a server-wide announcement about the new Saint
     * @param player The new Healer Saint
     */
    public void announceSaint(Player player) {
        String message = "\n" +
                ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★\n" +
                ChatColor.WHITE + "   🌟 " + ChatColor.GOLD + "A MIRACLE HAS HAPPENED!" + ChatColor.WHITE + " 🌟\n" +
                ChatColor.YELLOW + "   " + player.getName() + " has been chosen as the " +
                ChatColor.GOLD + "HEALER SAINT" + ChatColor.YELLOW + "!\n" +
                ChatColor.WHITE + "   Their holy powers will aid the server!\n" +
                ChatColor.GOLD + "★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★\n";

        Bukkit.broadcastMessage(message);

        // Play sound for all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
        }
    }

    /**
     * Apply visual effects to the Healer Saint
     * @param player The Healer Saint player
     */
    public void applySaintEffects(Player player) {
        // Add glowing effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 86400, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 86400, 3, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 86400, 3, false, false));
    }

    /**
     * Remove visual effects from a former Healer Saint
     * @param player The former Healer Saint player
     */
    public void removeSaintEffects(Player player) {
        // Remove glowing effect
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        player.removePotionEffect(PotionEffectType.REGENERATION);
    }
}