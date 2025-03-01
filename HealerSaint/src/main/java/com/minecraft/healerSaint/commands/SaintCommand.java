package com.minecraft.healerSaint.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.minecraft.healerSaint.HealerSaint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SaintCommand implements CommandExecutor, TabCompleter {
    private final HealerSaint plugin;

    public SaintCommand(HealerSaint plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if sender has permission
        if (!sender.hasPermission("saint.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // If no arguments, show help
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        // Handle subcommands
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                plugin.getSaintManager().reloadData();
                sender.sendMessage(ChatColor.GREEN + "HealerSaint config reloaded!");
                return true;

            case "give":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /saint give <player>");
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }

                plugin.getSaintManager().setSaint(target);
                sender.sendMessage(ChatColor.GREEN + target.getName() + " is now the Healer Saint!");
                return true;

            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /saint remove <player>");
                    return true;
                }

                Player removeTarget = Bukkit.getPlayer(args[1]);
                if (removeTarget == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found: " + args[1]);
                    return true;
                }

                if (!plugin.getSaintManager().isSaint(removeTarget)) {
                    sender.sendMessage(ChatColor.RED + removeTarget.getName() + " is not a Healer Saint!");
                    return true;
                }

                plugin.getSaintManager().removeSaint(removeTarget);
                sender.sendMessage(ChatColor.GREEN + "Removed " + removeTarget.getName() + " from being a Healer Saint.");
                return true;

            case "info":
                UUID currentSaintUUID = plugin.getSaintManager().getCurrentSaint();
                if (currentSaintUUID == null) {
                    sender.sendMessage(ChatColor.YELLOW + "There is currently no Healer Saint on the server.");
                } else {
                    Player saint = Bukkit.getPlayer(currentSaintUUID);
                    String saintName = saint != null ? saint.getName() : "Offline Saint (" + currentSaintUUID + ")";
                    sender.sendMessage(ChatColor.GOLD + "Current Healer Saint: " + ChatColor.YELLOW + saintName);
                }
                return true;

            default:
                showHelp(sender);
                return true;
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== HealerSaint Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/saint reload " + ChatColor.WHITE + "- Reload the plugin configuration");
        sender.sendMessage(ChatColor.YELLOW + "/saint give <player> " + ChatColor.WHITE + "- Make a player the Healer Saint");
        sender.sendMessage(ChatColor.YELLOW + "/saint remove <player> " + ChatColor.WHITE + "- Remove a player's Saint status");
        sender.sendMessage(ChatColor.YELLOW + "/saint info " + ChatColor.WHITE + "- Show who is the current Healer Saint");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("saint.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "give", "remove", "info").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("remove")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}