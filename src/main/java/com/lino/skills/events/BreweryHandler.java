package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;

public final class BreweryHandler implements Listener {

    private final SkillsPlugin plugin;

    public BreweryHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewComplete(BrewEvent event) {
        Location standLoc = event.getBlock().getLocation();
        ItemStack reagent = event.getContents().getIngredient();
        long xp = evaluateReagentXp(reagent != null ? reagent.getType() : Material.AIR);

        int highestPotionmaster = 0;
        for (Player nearby : standLoc.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(standLoc) <= 64.0) {
                plugin.getProgressionService().awardExperience(nearby, "alchemy", xp, true);
                int lvl = plugin.getAttributeService().resolveTalentLevel(nearby, "potionmaster");
                if (lvl > highestPotionmaster) {
                    highestPotionmaster = lvl;
                }
            }
        }

        if (highestPotionmaster > 0) {
            double factor = 1.0 + (highestPotionmaster * 0.04);
            List<ItemStack> bottles = event.getResults();
            for (ItemStack bottle : bottles) {
                if (bottle != null && bottle.getItemMeta() instanceof PotionMeta meta) {
                    List<PotionEffect> extended = new ArrayList<>();
                    if (meta.hasCustomEffects()) {
                        for (PotionEffect eff : meta.getCustomEffects()) {
                            int duration = (int) Math.round(eff.getDuration() * factor);
                            extended.add(new PotionEffect(eff.getType(), duration, eff.getAmplifier(), eff.isAmbient(), eff.hasParticles(), eff.hasIcon()));
                        }
                        meta.clearCustomEffects();
                    } else {
                        PotionType base = meta.getBasePotionType();
                        if (base != null) {
                            for (PotionEffect eff : base.getPotionEffects()) {
                                if (eff.getDuration() > 20) {
                                    int duration = (int) Math.round(eff.getDuration() * factor);
                                    extended.add(new PotionEffect(eff.getType(), duration, eff.getAmplifier(), eff.isAmbient(), eff.hasParticles(), eff.hasIcon()));
                                }
                            }
                        }
                    }
                    for (PotionEffect eff : extended) {
                        meta.addCustomEffect(eff, true);
                    }
                    bottle.setItemMeta(meta);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStandInteraction(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player alchemist)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof BrewingStand stand)) {
            return;
        }

        int efficient = plugin.getAttributeService().resolveTalentLevel(alchemist, "efficient_brewing");
        if (efficient <= 0) {
            return;
        }

        Location loc = stand.getLocation();
        SchedulerAdapter.scheduleDelayed(plugin, () -> {
            if (loc.getBlock().getState() instanceof BrewingStand activeStand) {
                int remaining = activeStand.getBrewingTime();
                if (remaining > 380) {
                    double scale = Math.max(0.25, 1.0 - (efficient * 0.015));
                    activeStand.setBrewingTime(Math.max(20, (int) Math.round(remaining * scale)));
                    activeStand.update();
                }
            }
        }, 2L);
    }

    private long evaluateReagentXp(Material mat) {
        return switch (mat) {
            case TURTLE_HELMET -> 20L;
            case DRAGON_BREATH, PHANTOM_MEMBRANE -> 12L;
            case RABBIT_FOOT, GLISTERING_MELON_SLICE, GOLDEN_CARROT, BLAZE_POWDER -> 6L;
            case PUFFERFISH, MAGMA_CREAM, GHAST_TEAR -> 4L;
            case SUGAR -> 3L;
            case SPIDER_EYE -> 2L;
            default -> 5L;
        };
    }
}
