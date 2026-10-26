package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SessionHandler implements Listener {

    private final SkillsPlugin plugin;

    public SessionHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getProfileRepository().loadProfileAsync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getProgressionService().dismissBanner(event.getPlayer());
        plugin.getProfileRepository().releaseProfile(event.getPlayer().getUniqueId());
    }
}
