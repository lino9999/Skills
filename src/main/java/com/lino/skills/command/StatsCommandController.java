package com.lino.skills.command;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.ui.AttributesGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class StatsCommandController implements CommandExecutor, TabCompleter {

    private final SkillsPlugin plugin;

    public StatsCommandController(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("players-only")));
            return true;
        }
        if (!player.hasPermission("skills.stats")) {
            player.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("access-denied")));
            return true;
        }
        AttributesGui.display(plugin, player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
