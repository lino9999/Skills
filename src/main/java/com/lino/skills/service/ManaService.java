package com.lino.skills.service;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ManaService {

    private final SkillsPlugin plugin;

    public ManaService(SkillsPlugin plugin) {
        this.plugin = plugin;
        SchedulerAdapter.scheduleRepeating(plugin, this::regenerateAllOnline, 20L, 20L);
    }

    private void regenerateAllOnline() {
        double ratio = plugin.getConfig().getDouble("mana-pool.regen-ratio-per-second", 0.02);
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
            if (profile == null) {
                continue;
            }
            double max = computeMaxMana(player);
            double current = profile.getMana();
            if (current < max) {
                profile.setMana(Math.min(max, current + Math.max(1.0, max * ratio)));
            }
        }
    }

    public double computeMaxMana(Player player) {
        double base = plugin.getConfig().getDouble("mana-pool.base-capacity", 100.0);
        double scaling = plugin.getConfig().getDouble("mana-pool.wisdom-scaling", 1.0);
        double wisdom = plugin.getAttributeService().resolveAttribute(player, "wisdom");
        return base + (wisdom * scaling);
    }
}
