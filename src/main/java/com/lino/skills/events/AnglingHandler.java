package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;

public final class AnglingHandler implements Listener {

    private static final List<Material> OCEANIC_LOOT = List.of(
            Material.NAUTILUS_SHELL,
            Material.HEART_OF_THE_SEA,
            Material.ENCHANTED_GOLDEN_APPLE,
            Material.DIAMOND,
            Material.NAME_TAG,
            Material.SADDLE
    );

    private final SkillsPlugin plugin;

    public AnglingHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReelIn(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player angler = event.getPlayer();
        plugin.getProgressionService().awardExperience(angler, "fishing", 25L, true);

        Entity hooked = event.getCaught();
        if (hooked == null) {
            return;
        }

        int depths = plugin.getAttributeService().resolveTalentLevel(angler, "eye_of_the_depths");
        if (depths > 0 && Math.random() * 100.0 < (depths * 0.5)) {
            Material prize = OCEANIC_LOOT.get((int) (Math.random() * OCEANIC_LOOT.size()));
            angler.getWorld().dropItemNaturally(hooked.getLocation(), new ItemStack(prize));
        }

        int magnetic = plugin.getAttributeService().resolveTalentLevel(angler, "magnetic_rod");
        if (magnetic > 0 && hooked instanceof Item) {
            Vector pull = angler.getLocation().toVector().subtract(hooked.getLocation().toVector()).normalize().multiply(1.45);
            hooked.setVelocity(pull);
        }
    }
}
