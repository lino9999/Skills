package com.lino.skills.domain;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerProfile {

    private final UUID playerId;
    private String username;
    private final Map<String, Integer> skillTiers = new ConcurrentHashMap<>();
    private final Map<String, Long> skillXp = new ConcurrentHashMap<>();
    private final Map<String, Double> bonusAttributes = new ConcurrentHashMap<>();
    private double mana = 100.0;
    private boolean hudVisible = true;
    private boolean audioEnabled = true;

    public PlayerProfile(UUID playerId, String username) {
        this.playerId = playerId;
        this.username = username;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getSkillTier(String skillId) {
        return skillTiers.getOrDefault(skillId.toLowerCase(Locale.ROOT), 0);
    }

    public void setSkillTier(String skillId, int tier) {
        skillTiers.put(skillId.toLowerCase(Locale.ROOT), Math.max(0, tier));
    }

    public long getSkillXp(String skillId) {
        return skillXp.getOrDefault(skillId.toLowerCase(Locale.ROOT), 0L);
    }

    public void setSkillXp(String skillId, long xp) {
        skillXp.put(skillId.toLowerCase(Locale.ROOT), Math.max(0L, xp));
    }

    public double getBonusAttribute(String attrId) {
        return bonusAttributes.getOrDefault(attrId.toLowerCase(Locale.ROOT), 0.0);
    }

    public void setBonusAttribute(String attrId, double amount) {
        bonusAttributes.put(attrId.toLowerCase(Locale.ROOT), amount);
    }

    public void addBonusAttribute(String attrId, double delta) {
        String key = attrId.toLowerCase(Locale.ROOT);
        bonusAttributes.put(key, bonusAttributes.getOrDefault(key, 0.0) + delta);
    }

    public double getMana() {
        return mana;
    }

    public void setMana(double mana) {
        this.mana = Math.max(0.0, mana);
    }

    public boolean isHudVisible() {
        return hudVisible;
    }

    public void setHudVisible(boolean hudVisible) {
        this.hudVisible = hudVisible;
    }

    public boolean isAudioEnabled() {
        return audioEnabled;
    }

    public void setAudioEnabled(boolean audioEnabled) {
        this.audioEnabled = audioEnabled;
    }

    public int getCombinedTier() {
        int sum = 0;
        for (int tier : skillTiers.values()) {
            sum += tier;
        }
        return sum;
    }

    public double getAverageTier(int totalRegisteredSkills) {
        if (totalRegisteredSkills <= 0) {
            return 0.0;
        }
        return (double) getCombinedTier() / totalRegisteredSkills;
    }

    public Map<String, Integer> getSkillTiersMap() {
        return skillTiers;
    }

    public Map<String, Long> getSkillXpMap() {
        return skillXp;
    }

    public Map<String, Double> getBonusAttributesMap() {
        return bonusAttributes;
    }
}
