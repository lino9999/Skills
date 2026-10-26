package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public final class SlayerHandler implements Listener {

    private static final String CAGE_SPAWN_FLAG = "skills_cage_spawned";
    private final SkillsPlugin plugin;

    public SlayerHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            event.getEntity().setMetadata(CAGE_SPAWN_FLAG, new FixedMetadataValue(plugin, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobSlain(EntityDeathEvent event) {
        LivingEntity deceased = event.getEntity();
        Player slayer = deceased.getKiller();
        if (slayer == null || deceased.hasMetadata(CAGE_SPAWN_FLAG)) {
            return;
        }

        AttributeInstance maxHp = deceased.getAttribute(Attribute.MAX_HEALTH);
        double hp = maxHp != null ? maxHp.getValue() : 20.0;
        long xp = Math.max(1L, Math.round(hp * 0.35));
        plugin.getProgressionService().awardExperience(slayer, "combat", xp, true);
    }
}
