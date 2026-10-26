package com.lino.skills.service;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.List;

public final class HudService {

    private final SkillsPlugin plugin;

    public HudService(SkillsPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("hud-bar.enabled", true)) {
            long ticks = plugin.getConfig().getLong("hud-bar.refresh-ticks", 10L);
            SchedulerAdapter.scheduleRepeating(plugin, this::broadcastHud, ticks, ticks);
        }
    }

    private void broadcastHud() {
        String tpl = plugin.getConfig().getString("hud-bar.template",
                "&c♥ &f{hp}&8/&f{max_hp}     &#f472b6🛡 &f{attr_defense}  &8|  &#ef4444⚔ &f{attr_strength}  &8|  &#34d399✦ &f{attr_speed}  &8|  &#38bdf8💧 &f{mana}&8/&f{max_mana}");
        List<String> ignoredWorlds = plugin.getConfig().getStringList("hud-bar.ignored-worlds");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (ignoredWorlds.contains(player.getWorld().getName())) {
                continue;
            }
            PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
            if (profile == null || !profile.isHudVisible()) {
                continue;
            }

            double hp = player.getHealth();
            AttributeInstance maxHpAttr = player.getAttribute(Attribute.MAX_HEALTH);
            double maxHp = maxHpAttr != null ? maxHpAttr.getValue() : 20.0;

            double def = plugin.getAttributeService().resolveAttribute(player, "defense");
            double str = plugin.getAttributeService().resolveAttribute(player, "strength");
            double spd = plugin.getAttributeService().resolveAttribute(player, "speed");
            double mana = profile.getMana();
            double maxMana = plugin.getManaService().computeMaxMana(player);

            String rendered = tpl
                    .replace("{hp}", ColorFormatter.formatWhole((long) Math.ceil(hp)))
                    .replace("{max_hp}", ColorFormatter.formatWhole((long) Math.ceil(maxHp)))
                    .replace("{attr_defense}", ColorFormatter.formatDecimal(def))
                    .replace("{attr_strength}", ColorFormatter.formatDecimal(str))
                    .replace("{attr_speed}", ColorFormatter.formatDecimal(spd))
                    .replace("{mana}", ColorFormatter.formatWhole((long) mana))
                    .replace("{max_mana}", ColorFormatter.formatWhole((long) maxMana));

            player.sendActionBar(ColorFormatter.render(rendered));
        }
    }
}
