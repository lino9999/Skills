package com.lino.skills.command;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.AttributeDefinition;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.SkillDefinition;
import com.lino.skills.platform.ColorFormatter;
import org.bukkit.Bukkit;
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

public final class AdminCommandController implements CommandExecutor, TabCompleter {

    private final SkillsPlugin plugin;

    public AdminCommandController(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("skills.admin")) {
            sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("access-denied")));
            return true;
        }

        if (args.length == 0) {
            sendAdminGuide(sender);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "reload" -> {
                long t0 = System.currentTimeMillis();
                plugin.getProgressionService().dismissAllBanners();
                plugin.getConfigRegistry().reloadAll();
                long elapsed = System.currentTimeMillis() - t0;

                String msg = plugin.getConfigRegistry().formatMessage("config-reloaded")
                        .replace("{ms}", String.valueOf(elapsed))
                        .replace("{skills}", String.valueOf(plugin.getConfigRegistry().getSkills().size()))
                        .replace("{attributes}", String.valueOf(plugin.getConfigRegistry().getAttributes().size()))
                        .replace("{talents}", String.valueOf(plugin.getConfigRegistry().getTalents().size()));
                sender.sendMessage(ColorFormatter.render(msg));
            }
            case "add", "give" -> {
                if (args.length < 4) {
                    sender.sendMessage(ColorFormatter.render("&#f43f5eUsage: /skillsadmin add <player> <skill|attribute|mana> <amount>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("player-not-found")));
                    return true;
                }
                String key = args[2].toLowerCase(Locale.ROOT);
                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("invalid-number")));
                    return true;
                }

                PlayerProfile profile = plugin.getProfileRepository().getProfile(target);
                if (profile == null) {
                    return true;
                }

                if (key.equals("mana")) {
                    profile.setMana(profile.getMana() + amount);
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("added-mana")
                            .replace("{player}", target.getName())
                            .replace("{amount}", ColorFormatter.formatDecimal(amount))));
                    return true;
                }

                SkillDefinition skill = plugin.getConfigRegistry().getSkill(key);
                if (skill != null) {
                    plugin.getProgressionService().awardExperience(target, skill.id(), Math.round(amount), true);
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("added-xp")
                            .replace("{player}", target.getName())
                            .replace("{amount}", ColorFormatter.formatDecimal(amount))
                            .replace("{skill}", skill.title())));
                    return true;
                }

                AttributeDefinition attr = plugin.getConfigRegistry().getAttribute(key);
                if (attr != null) {
                    profile.addBonusAttribute(attr.id(), amount);
                    plugin.getAttributeService().synchronizePlayer(target);
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("added-attribute")
                            .replace("{player}", target.getName())
                            .replace("{amount}", ColorFormatter.formatDecimal(amount))
                            .replace("{attribute}", attr.title())));
                    return true;
                }

                sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("unknown-target")));
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage(ColorFormatter.render("&#f43f5eUsage: /skillsadmin set <player> <skill|attribute> <value>"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("player-not-found")));
                    return true;
                }
                String key = args[2].toLowerCase(Locale.ROOT);
                double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("invalid-number")));
                    return true;
                }

                PlayerProfile profile = plugin.getProfileRepository().getProfile(target);
                if (profile == null) {
                    return true;
                }

                SkillDefinition skill = plugin.getConfigRegistry().getSkill(key);
                if (skill != null) {
                    int tier = Math.max(0, Math.min(skill.maxTier(), (int) amount));
                    profile.setSkillTier(skill.id(), tier);
                    long cumulativeXp = 0L;
                    for (int i = 1; i <= tier; i++) {
                        cumulativeXp += skill.thresholdForTier(i);
                    }
                    profile.setSkillXp(skill.id(), cumulativeXp);
                    plugin.getAttributeService().synchronizePlayer(target);
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("set-tier")
                            .replace("{player}", target.getName())
                            .replace("{amount}", String.valueOf(tier))
                            .replace("{skill}", skill.title())));
                    return true;
                }

                AttributeDefinition attr = plugin.getConfigRegistry().getAttribute(key);
                if (attr != null) {
                    profile.setBonusAttribute(attr.id(), amount);
                    plugin.getAttributeService().synchronizePlayer(target);
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("set-attribute")
                            .replace("{player}", target.getName())
                            .replace("{amount}", ColorFormatter.formatDecimal(amount))
                            .replace("{attribute}", attr.title())));
                    return true;
                }

                sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("unknown-target")));
            }
            case "wipe", "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(ColorFormatter.render("&#f43f5eUsage: /skillsadmin wipe <player|all>"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    plugin.getProfileRepository().wipeAllProfiles();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        plugin.getProfileRepository().loadProfileAsync(online);
                    }
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("wiped-everyone")));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("player-not-found")));
                    return true;
                }
                plugin.getProfileRepository().wipeProfile(target.getUniqueId());
                plugin.getProfileRepository().loadProfileAsync(target);
                sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("wiped-profile")
                        .replace("{player}", target.getName())));
            }
            case "sync", "recount" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        plugin.getAttributeService().synchronizePlayer(online);
                    }
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("synced-everyone")));
                    return true;
                }
                Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : (sender instanceof Player p ? p : null);
                if (target == null) {
                    sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("player-not-found")));
                    return true;
                }
                plugin.getAttributeService().synchronizePlayer(target);
                sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("synced-profile")
                        .replace("{player}", target.getName())));
            }
            default -> sendAdminGuide(sender);
        }
        return true;
    }

    private void sendAdminGuide(CommandSender sender) {
        sender.sendMessage(ColorFormatter.render("&8&m──────────────&r &#e879f9&lSkills Admin &8&m──────────────"));
        sender.sendMessage(ColorFormatter.render(" &d/skillsadmin reload &8• &7Reload all YAML configurations"));
        sender.sendMessage(ColorFormatter.render(" &d/skillsadmin add <player> <skill|attribute|mana> <amount> &8• &7Grant progression"));
        sender.sendMessage(ColorFormatter.render(" &d/skillsadmin set <player> <skill|attribute> <value> &8• &7Set exact tier/bonus"));
        sender.sendMessage(ColorFormatter.render(" &d/skillsadmin wipe <player|all> &8• &7Wipe progression profile"));
        sender.sendMessage(ColorFormatter.render(" &d/skillsadmin sync [player|all] &8• &7Synchronize RPG attributes"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("skills.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(List.of("reload", "add", "set", "wipe", "sync"));
            subcommands.removeIf(s -> !s.startsWith(args[0].toLowerCase(Locale.ROOT)));
            return subcommands;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            if (sub.equals("wipe") || sub.equals("reset") || sub.equals("sync") || sub.equals("recount")) {
                names.add("all");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                names.add(online.getName());
            }
            names.removeIf(n -> !n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)));
            return names;
        }

        if (args.length == 3 && (sub.equals("add") || sub.equals("give") || sub.equals("set"))) {
            List<String> targets = new ArrayList<>();
            targets.addAll(plugin.getConfigRegistry().getSkills().keySet());
            targets.addAll(plugin.getConfigRegistry().getAttributes().keySet());
            if (sub.equals("add") || sub.equals("give")) {
                targets.add("mana");
            }
            targets.removeIf(t -> !t.startsWith(args[2].toLowerCase(Locale.ROOT)));
            return targets;
        }

        return List.of();
    }
}
