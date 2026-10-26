package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public final class DurabilityHandler implements Listener {

    private final SkillsPlugin plugin;

    public DurabilityHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGearWear(PlayerItemDamageEvent event) {
        Player holder = event.getPlayer();
        ItemStack tool = event.getItem();

        int craftsmanship = plugin.getAttributeService().resolveTalentLevel(holder, "craftsmanship");
        if (craftsmanship > 0 && tool.getType().name().endsWith("_AXE")) {
            if (Math.random() * 100.0 < (craftsmanship * 0.5)) {
                event.setCancelled(true);
                return;
            }
        }

        int secondChance = plugin.getAttributeService().resolveTalentLevel(holder, "second_chance");
        if (secondChance > 0 && tool.getItemMeta() instanceof Damageable dmgMeta) {
            int max = tool.getType().getMaxDurability();
            int remaining = max - dmgMeta.getDamage();
            if (remaining <= 30 && Math.random() * 100.0 < (secondChance * 0.1)) {
                dmgMeta.setDamage(0);
                tool.setItemMeta(dmgMeta);
                event.setCancelled(true);
                try {
                    holder.playSound(holder.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
