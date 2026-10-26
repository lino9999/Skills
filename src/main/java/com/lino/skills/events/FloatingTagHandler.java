package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.List;
import java.util.Locale;

public final class FloatingTagHandler implements Listener {

    private final SkillsPlugin plugin;

    public FloatingTagHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHitTag(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("combat-tags.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        List<String> ignored = plugin.getConfig().getStringList("combat-tags.ignored-mob-types");
        if (ignored.contains(target.getType().name().toLowerCase(Locale.ROOT))) {
            return;
        }

        if (event.getDamager() instanceof Player attacker) {
            boolean postMitigation = plugin.getConfig().getBoolean("combat-tags.use-post-mitigation", false);
            double dmg = postMitigation ? event.getFinalDamage() : event.getDamage();
            boolean crit = plugin.getAttributeService().rollCriticalHit(attacker);

            String tpl = crit
                    ? plugin.getConfig().getString("combat-tags.critical-template", "&#f43f5e✦ &l-{value} &#f43f5e✦")
                    : plugin.getConfig().getString("combat-tags.regular-template", "&7-{value}");

            String label = tpl.replace("{value}", ColorFormatter.formatDecimal(dmg));
            spawnHologram(target.getLocation().add(0, target.getHeight() * 0.75, 0), label);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRestoreTag(EntityRegainHealthEvent event) {
        if (!plugin.getConfig().getBoolean("combat-tags.enabled", true)
                || !plugin.getConfig().getBoolean("combat-tags.restore-enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target) || event.getAmount() < 0.5) {
            return;
        }
        List<String> ignored = plugin.getConfig().getStringList("combat-tags.ignored-mob-types");
        if (ignored.contains(target.getType().name().toLowerCase(Locale.ROOT))) {
            return;
        }

        String tpl = plugin.getConfig().getString("combat-tags.restore-template", "&#4ade80+{value}");
        String label = tpl.replace("{value}", ColorFormatter.formatDecimal(event.getAmount()));
        spawnHologram(target.getLocation().add(0, target.getHeight() * 0.75, 0), label);
    }

    private void spawnHologram(Location origin, String label) {
        double hSpread = plugin.getConfig().getDouble("combat-tags.spread-horizontal", 0.45);
        double vSpread = plugin.getConfig().getDouble("combat-tags.spread-vertical", 0.55);

        double dx = (Math.random() - 0.5) * hSpread * 2.0;
        double dy = (Math.random() - 0.5) * vSpread;
        double dz = (Math.random() - 0.5) * hSpread * 2.0;
        Location point = origin.clone().add(dx, 0.2 + dy, dz);

        SchedulerAdapter.executeAtLocation(plugin, point, () -> {
            try {
                TextDisplay tag = point.getWorld().spawn(point, TextDisplay.class, td -> {
                    td.text(ColorFormatter.render(label));
                    td.setBillboard(Display.Billboard.CENTER);
                    td.setShadowed(true);
                    td.setViewRange(30.0f);
                });

                SchedulerAdapter.scheduleDelayed(plugin, () -> {
                    if (tag.isValid()) {
                        tag.remove();
                    }
                }, 16L);
            } catch (Exception ignored) {
            }
        });
    }
}
