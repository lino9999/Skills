package com.lino.skills.service;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.SkillDefinition;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

public final class AttributeService {

    private final SkillsPlugin plugin;

    public AttributeService(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public double resolveAttribute(Player player, String attrId) {
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return 0.0;
        }
        double total = profile.getBonusAttribute(attrId);
        for (SkillDefinition skill : plugin.getConfigRegistry().getSkills().values()) {
            int tier = profile.getSkillTier(skill.id());
            if (tier > 0) {
                total += skill.resolveGrantAtTier(attrId, tier);
            }
        }
        return total;
    }

    public int resolveTalentLevel(Player player, String talentId) {
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return 0;
        }
        int total = 0;
        for (SkillDefinition skill : plugin.getConfigRegistry().getSkills().values()) {
            int tier = profile.getSkillTier(skill.id());
            if (tier > 0) {
                total += skill.resolveGrantAtTier(talentId, tier);
            }
        }
        return total;
    }

    public void synchronizePlayer(Player player) {
        double vitality = resolveAttribute(player, "health");
        double agility = resolveAttribute(player, "speed");
        double swiftness = resolveAttribute(player, "attack_speed");

        AttributeInstance maxHp = getPlayerAttribute(player, "MAX_HEALTH", "GENERIC_MAX_HEALTH");
        if (maxHp != null) {
            maxHp.setBaseValue(20.0 + (vitality * 0.5));
        }
        if (plugin.getConfig().getBoolean("hud-bar.normalize-hearts", true)) {
            player.setHealthScaled(true);
            player.setHealthScale(20.0);
        }

        AttributeInstance moveSpeed = getPlayerAttribute(player, "MOVEMENT_SPEED", "GENERIC_MOVEMENT_SPEED");
        if (moveSpeed != null) {
            double multiplier = 1.0 + Math.min(2.0, agility * 0.0015);
            moveSpeed.setBaseValue(0.1 * multiplier);
        }

        AttributeInstance atkSpeed = getPlayerAttribute(player, "ATTACK_SPEED", "GENERIC_ATTACK_SPEED");
        if (atkSpeed != null) {
            double multiplier = 1.0 + Math.min(2.0, swiftness * 0.002);
            atkSpeed.setBaseValue(4.0 * multiplier);
        }
    }

    @SuppressWarnings("removal")
    private AttributeInstance getPlayerAttribute(Player player, String... candidateNames) {
        for (String name : candidateNames) {
            try {
                Attribute attr = Attribute.valueOf(name);
                AttributeInstance instance = player.getAttribute(attr);
                if (instance != null) {
                    return instance;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return null;
    }

    public double computeOutboundDamage(Player attacker, double rawDamage) {
        double strength = resolveAttribute(attacker, "strength");
        return rawDamage * (1.0 + (strength * 0.0025));
    }

    public double computeInboundDamage(Player defender, double rawDamage) {
        double defense = resolveAttribute(defender, "defense");
        double reduction = Math.min(0.80, defense * 0.0025);
        return rawDamage * (1.0 - reduction);
    }

    public boolean rollCriticalHit(Player attacker) {
        double critChance = Math.min(100.0, resolveAttribute(attacker, "crit_chance") * 0.15);
        return Math.random() * 100.0 < critChance;
    }

    public double computeCriticalMultiplier(Player attacker) {
        double critDamage = resolveAttribute(attacker, "crit_damage");
        return 1.5 + (critDamage * 0.004);
    }

    public boolean rollFerocityStrike(Player attacker) {
        double ferocityChance = Math.min(100.0, resolveAttribute(attacker, "ferocity") * 0.2);
        return Math.random() * 100.0 < ferocityChance;
    }

    public double computeWisdomXpMultiplier(Player player) {
        double wisdom = resolveAttribute(player, "wisdom");
        return 1.0 + (wisdom * 0.005);
    }
}
