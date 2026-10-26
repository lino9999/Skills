package com.lino.skills.command;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.ui.OverviewGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PlayerCommandController implements CommandExecutor, TabCompleter {

    private final SkillsPlugin plugin;
    private final AdminCommandController adminDelegate;

    public PlayerCommandController(SkillsPlugin plugin, AdminCommandController adminDelegate) {
        this.plugin = plugin;
        this.adminDelegate = adminDelegate;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "rank", "top", "leaderboard" -> {
                    if (!sender.hasPermission("skills.rank")) {
                        sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("access-denied")));
                        return true;
                    }
                    plugin.getRankingService().displayRankings(sender);
                    return true;
                }
                case "hud", "actionbar", "toggleactionbar" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("players-only")));
                        return true;
                    }
                    if (!player.hasPermission("skills.hud")) {
                        player.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("access-denied")));
                        return true;
                    }
                    PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
                    if (profile != null) {
                        boolean next = !profile.isHudVisible();
                        profile.setHudVisible(next);
                        player.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage(next ? "hud-toggled-on" : "hud-toggled-off")));
                    }
                    return true;
                }
                case "audio", "sound", "togglexpgainsound" -> {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("players-only")));
                        return true;
                    }
                    if (!player.hasPermission("skills.audio")) {
                        player.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("access-denied")));
                        return true;
                    }
                    PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
                    if (profile != null) {
                        boolean next = !profile.isAudioEnabled();
                        profile.setAudioEnabled(next);
                        player.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage(next ? "audio-toggled-on" : "audio-toggled-off")));
                    }
                    return true;
                }
                case "reload", "add", "give", "set", "wipe", "reset", "sync", "recount" -> {
                    return adminDelegate.onCommand(sender, command, label, args);
                }
                case "help" -> {
                    sendCommandGuide(sender);
                    return true;
                }
                default -> {
                }
            }
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("players-only")));
            return true;
        }
        if (!player.hasPermission("skills.menu")) {
            player.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("access-denied")));
            return true;
        }

        OverviewGui.display(plugin, player);
        return true;
    }

    private void sendCommandGuide(CommandSender sender) {
        sender.sendMessage(ColorFormatter.render("&8&m──────────────&r &#e879f9&lSkills Guide &8&m──────────────"));
        sender.sendMessage(ColorFormatter.render(" &d/skills &8• &7Open your interactive Skills Overview"));
        sender.sendMessage(ColorFormatter.render(" &d/stats &8• &7Inspect your active RPG Attributes"));
        sender.sendMessage(ColorFormatter.render(" &d/skills rank &8• &7View the server progression rankings"));
        sender.sendMessage(ColorFormatter.render(" &d/skills hud &8• &7Toggle the persistent Action Bar HUD"));
        sender.sendMessage(ColorFormatter.render(" &d/skills audio &8• &7Toggle experience gain sound effects"));
        if (sender.hasPermission("skills.admin")) {
            sender.sendMessage(ColorFormatter.render(" &d/skillsadmin &8• &7Administrative progression suite"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("rank", "hud", "audio", "help"));
            if (sender.hasPermission("skills.admin")) {
                options.addAll(List.of("reload", "add", "set", "wipe", "sync"));
            }
            options.removeIf(opt -> !opt.startsWith(args[0].toLowerCase(Locale.ROOT)));
            return options;
        }
        if (args.length > 1 && sender.hasPermission("skills.admin")) {
            return adminDelegate.onTabComplete(sender, command, label, args);
        }
        return List.of();
    }
}
