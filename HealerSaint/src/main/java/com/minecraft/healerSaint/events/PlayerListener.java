package com.minecraft.healerSaint.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.minecraft.healerSaint.HealerSaint;

import java.util.UUID;

public class PlayerListener implements Listener {
    private final HealerSaint plugin;

    public PlayerListener(HealerSaint plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if this player should become a saint
        boolean becameSaint = plugin.getSaintManager().checkNewSaint(player);

        // If player is already a saint but doesn't have effects (e.g., after restart)
        UUID currentSaint = plugin.getSaintManager().getCurrentSaint();
        if (currentSaint != null && player.getUniqueId().equals(currentSaint) && !becameSaint) {
            plugin.applySaintEffects(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // If player is a saint, remove visual effects (but don't revoke saint status)
        if (plugin.getSaintManager().isSaint(player)) {
            plugin.removeSaintEffects(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // If player is a saint, format their chat messages
        if (plugin.getSaintManager().isSaint(player)) {
            String format = ChatColor.GOLD + "[Saint] " + ChatColor.WHITE + "%s" +
                    ChatColor.RESET + ": " + ChatColor.YELLOW + "%s";
            event.setFormat(format);
        }
    }
}