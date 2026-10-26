package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RoamHandler implements Listener {

    private final SkillsPlugin plugin;
    private final Map<UUID, Double> stepAccumulator = new ConcurrentHashMap<>();

    public RoamHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStep(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        double delta = from.distance(to);
        if (delta > 10.0) {
            return;
        }

        Player explorer = event.getPlayer();
        double total = stepAccumulator.getOrDefault(explorer.getUniqueId(), 0.0) + delta;
        if (total >= 45.0) {
            stepAccumulator.put(explorer.getUniqueId(), 0.0);
            plugin.getProgressionService().awardExperience(explorer, "exploration", 5L, true);
        } else {
            stepAccumulator.put(explorer.getUniqueId(), total);
        }
    }
}
