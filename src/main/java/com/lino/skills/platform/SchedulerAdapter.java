package com.lino.skills.platform;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public final class SchedulerAdapter {

    private static final boolean FOLIA_DETECTED;

    static {
        boolean detected;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            detected = true;
        } catch (ClassNotFoundException ex) {
            detected = false;
        }
        FOLIA_DETECTED = detected;
    }

    private SchedulerAdapter() {
    }

    public static void executeAsync(Plugin plugin, Runnable action) {
        if (FOLIA_DETECTED) {
            Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> action.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, action);
        }
    }

    public static void scheduleAsyncRepeating(Plugin plugin, Runnable action, long initialDelayTicks, long intervalTicks) {
        if (FOLIA_DETECTED) {
            Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> action.run(),
                    initialDelayTicks * 50L,
                    intervalTicks * 50L,
                    TimeUnit.MILLISECONDS
            );
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, action, initialDelayTicks, intervalTicks);
        }
    }

    public static void executeMain(Plugin plugin, Runnable action) {
        if (FOLIA_DETECTED) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> action.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }

    public static void scheduleDelayed(Plugin plugin, Runnable action, long delayTicks) {
        if (FOLIA_DETECTED) {
            Bukkit.getGlobalRegionScheduler().runDelayed(
                    plugin,
                    scheduledTask -> action.run(),
                    Math.max(1L, delayTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, action, delayTicks);
        }
    }

    public static void scheduleRepeating(Plugin plugin, Runnable action, long initialDelayTicks, long intervalTicks) {
        if (FOLIA_DETECTED) {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    scheduledTask -> action.run(),
                    Math.max(1L, initialDelayTicks),
                    Math.max(1L, intervalTicks)
            );
        } else {
            Bukkit.getScheduler().runTaskTimer(plugin, action, initialDelayTicks, intervalTicks);
        }
    }

    public static void executeAtEntity(Plugin plugin, Entity target, Runnable action) {
        if (FOLIA_DETECTED) {
            target.getScheduler().run(plugin, scheduledTask -> action.run(), null);
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }

    public static void executeAtLocation(Plugin plugin, Location location, Runnable action) {
        if (FOLIA_DETECTED) {
            Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> action.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}
