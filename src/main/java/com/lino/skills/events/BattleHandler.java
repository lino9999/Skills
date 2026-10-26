package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Boss;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public final class BattleHandler implements Listener {

    private static final Set<PotionEffectType> DEBUFF_TYPES = Set.of(
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NAUSEA,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.HUNGER,
            PotionEffectType.DARKNESS,
            PotionEffectType.LEVITATION,
            PotionEffectType.UNLUCK
    );

    private final SkillsPlugin plugin;

    public BattleHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombatExchange(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            processOffensiveStrike(attacker, event);
        }
        if (event.getEntity() instanceof Player defender && event.getDamager() instanceof Boss) {
            int bravery = plugin.getAttributeService().resolveTalentLevel(defender, "bravery");
            if (bravery > 0) {
                double factor = 1.0 - Math.min(0.50, bravery * 0.01);
                event.setDamage(event.getDamage() * factor);
            }
        }
    }

    private void processOffensiveStrike(Player attacker, EntityDamageByEntityEvent event) {
        double damage = plugin.getAttributeService().computeOutboundDamage(attacker, event.getDamage());

        int versatile = plugin.getAttributeService().resolveTalentLevel(attacker, "versatile_tools");
        if (versatile > 0 && attacker.getInventory().getItemInMainHand().getType().name().endsWith("_PICKAXE")) {
            damage *= (1.0 + (versatile * 0.04));
        }

        int strongImpact = plugin.getAttributeService().resolveTalentLevel(attacker, "strong_impact");
        if (strongImpact > 0 && Math.random() * 100.0 < (strongImpact * 0.5)) {
            damage *= 3.0;
            attacker.getWorld().spawnParticle(Particle.CRIT, event.getEntity().getLocation().add(0, 1, 0), 18);
        }

        if (plugin.getAttributeService().rollCriticalHit(attacker)) {
            damage *= plugin.getAttributeService().computeCriticalMultiplier(attacker);
            attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, event.getEntity().getLocation().add(0, 1, 0), 14);
            try {
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            } catch (Exception ignored) {
            }
        }

        event.setDamage(damage);

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        int dazzle = plugin.getAttributeService().resolveTalentLevel(attacker, "dazzle");
        if (dazzle > 0 && Math.random() * 100.0 < (dazzle * 0.5)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 18 + (dazzle * 2), 1));
        }

        int serrated = plugin.getAttributeService().resolveTalentLevel(attacker, "serrated_strikes");
        if (serrated > 0 && Math.random() * 100.0 < (serrated * 0.5)) {
            scheduleBleedTicks(victim);
        }

        int endangering = plugin.getAttributeService().resolveTalentLevel(attacker, "endangering");
        if (endangering > 0 && Math.random() * 100.0 < Math.min(100.0, endangering * 3.0)) {
            SchedulerAdapter.executeAtEntity(plugin, victim, () -> {
                if (victim.isValid() && !victim.isDead()) {
                    victim.setNoDamageTicks(0);
                }
            });
        }

        if (plugin.getAttributeService().rollFerocityStrike(attacker)) {
            double bonusHit = damage;
            SchedulerAdapter.executeAtEntity(plugin, victim, () -> {
                if (victim.isValid() && !victim.isDead()) {
                    victim.damage(bonusHit, attacker);
                    victim.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victim.getLocation().add(0, 1, 0), 2);
                    try {
                        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        int dodging = plugin.getAttributeService().resolveTalentLevel(player, "dodging");
        if (dodging > 0 && Math.random() * 100.0 < (dodging * 0.5)) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.POOF, player.getLocation().add(0, 1, 0), 6);
            return;
        }

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA) {
            int infernal = plugin.getAttributeService().resolveTalentLevel(player, "infernal_resistance");
            if (infernal > 0 && Math.random() * 100.0 < (infernal * 0.5)) {
                event.setCancelled(true);
                return;
            }
        }

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            int seamless = plugin.getAttributeService().resolveTalentLevel(player, "seamless_movement");
            if (seamless > 0 && Math.random() * 100.0 < (seamless * 2.0)) {
                event.setCancelled(true);
                return;
            }
        }

        event.setDamage(plugin.getAttributeService().computeInboundDamage(player, event.getDamage()));

        int escape = plugin.getAttributeService().resolveTalentLevel(player, "accelerated_escape");
        if (escape > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30, Math.max(0, escape / 10), true, false, false));
        }

        if (cause != EntityDamageEvent.DamageCause.KILL
                && cause != EntityDamageEvent.DamageCause.SUICIDE
                && cause != EntityDamageEvent.DamageCause.CONTACT) {
            long armoryXp = Math.max(1L, Math.round(event.getFinalDamage() * 1.5));
            plugin.getProgressionService().awardExperience(player, "armory", armoryXp, false);
        }

        if (cause == EntityDamageEvent.DamageCause.FALL) {
            long explorationXp = Math.max(1L, Math.round(event.getFinalDamage() * 2.0));
            plugin.getProgressionService().awardExperience(player, "exploration", explorationXp, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPotionApplied(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED && event.getAction() != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }
        PotionEffect incoming = event.getNewEffect();
        if (incoming == null || !DEBUFF_TYPES.contains(incoming.getType())) {
            return;
        }

        int resilience = plugin.getAttributeService().resolveTalentLevel(player, "mystic_resilience");
        if (resilience > 0 && Math.random() * 100.0 < (resilience * 0.5)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHungerDeplete(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getFoodLevel() >= player.getFoodLevel()) {
            return;
        }

        int satiation = plugin.getAttributeService().resolveTalentLevel(player, "satiation");
        if (satiation > 0 && Math.random() * 100.0 < (satiation * 2.0)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHealthRestore(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        int shamanism = plugin.getAttributeService().resolveTalentLevel(player, "shamanism");
        if (shamanism > 0) {
            event.setAmount(event.getAmount() * (1.0 + (shamanism * 0.04)));
        }
    }

    private void scheduleBleedTicks(LivingEntity victim) {
        for (int pulse = 1; pulse <= 3; pulse++) {
            SchedulerAdapter.scheduleDelayed(plugin, () -> {
                if (victim.isValid() && !victim.isDead()) {
                    victim.damage(1.0);
                    victim.getWorld().spawnParticle(
                            Particle.DUST,
                            victim.getLocation().add(0, 1, 0),
                            8,
                            new Particle.DustOptions(Color.RED, 1.0f)
                    );
                }
            }, pulse * 20L);
        }
    }
}
