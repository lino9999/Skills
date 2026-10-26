package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.inventory.ItemStack;

public final class EnchantmentHandler implements Listener {

    private final SkillsPlugin plugin;

    public EnchantmentHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player enchanter = event.getEnchanter();
        int spentLevels = event.getExpLevelCost();

        plugin.getProgressionService().awardExperience(enchanter, "enchanting", spentLevels * 15L, true);

        int reimbursement = plugin.getAttributeService().resolveTalentLevel(enchanter, "reimbursement");
        if (reimbursement > 0 && Math.random() * 100.0 < (reimbursement * 0.5)) {
            SchedulerAdapter.executeAtEntity(plugin, enchanter, () -> enchanter.giveExpLevels(Math.max(1, spentLevels / 2)));
        }

        int overcompensation = plugin.getAttributeService().resolveTalentLevel(enchanter, "overcompensation");
        if (overcompensation > 0 && Math.random() * 100.0 < (overcompensation * 0.5)) {
            SchedulerAdapter.executeAtEntity(plugin, enchanter, () -> enchanter.getInventory().addItem(new ItemStack(Material.LAPIS_LAZULI, 3)));
        }
    }
}
