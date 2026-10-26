package com.lino.skills.persistence;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.RankEntry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class YamlProfileStore implements ProfileStore {

    private final SkillsPlugin plugin;
    private File file;
    private YamlConfiguration yaml;

    public YamlProfileStore(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public synchronized void initialize() {
        file = new File(plugin.getDataFolder(), "profiles.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public synchronized PlayerProfile fetchProfile(UUID playerId, String username) {
        PlayerProfile profile = new PlayerProfile(playerId, username);
        String path = "accounts." + playerId;
        if (!yaml.contains(path)) {
            return profile;
        }
        profile.setMana(yaml.getDouble(path + ".mana", 100.0));
        profile.setHudVisible(yaml.getBoolean(path + ".hud-visible", true));
        profile.setAudioEnabled(yaml.getBoolean(path + ".audio-enabled", true));

        ConfigurationSection skillSec = yaml.getConfigurationSection(path + ".skills");
        if (skillSec != null) {
            for (String sId : skillSec.getKeys(false)) {
                profile.setSkillTier(sId, skillSec.getInt(sId + ".tier", 0));
                profile.setSkillXp(sId, skillSec.getLong(sId + ".xp", 0L));
            }
        }

        ConfigurationSection attrSec = yaml.getConfigurationSection(path + ".attributes");
        if (attrSec != null) {
            for (String aId : attrSec.getKeys(false)) {
                profile.setBonusAttribute(aId, attrSec.getDouble(aId, 0.0));
            }
        }
        return profile;
    }

    @Override
    public synchronized void persistProfile(PlayerProfile profile) {
        String path = "accounts." + profile.getPlayerId();
        yaml.set(path + ".username", profile.getUsername());
        yaml.set(path + ".mana", profile.getMana());
        yaml.set(path + ".hud-visible", profile.isHudVisible());
        yaml.set(path + ".audio-enabled", profile.isAudioEnabled());
        yaml.set(path + ".combined-tier", profile.getCombinedTier());

        for (String sId : plugin.getConfigRegistry().getSkills().keySet()) {
            yaml.set(path + ".skills." + sId + ".tier", profile.getSkillTier(sId));
            yaml.set(path + ".skills." + sId + ".xp", profile.getSkillXp(sId));
        }

        for (Map.Entry<String, Double> entry : profile.getBonusAttributesMap().entrySet()) {
            yaml.set(path + ".attributes." + entry.getKey(), entry.getValue());
        }

        flushToFile();
    }

    @Override
    public synchronized void deleteProfile(UUID playerId) {
        yaml.set("accounts." + playerId, null);
        flushToFile();
    }

    @Override
    public synchronized void deleteAllProfiles() {
        yaml.set("accounts", null);
        flushToFile();
    }

    @Override
    public synchronized List<RankEntry> queryTopProfiles(int maxResults) {
        List<RankEntry> list = new ArrayList<>();
        ConfigurationSection sec = yaml.getConfigurationSection("accounts");
        if (sec == null) {
            return list;
        }
        for (String key : sec.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String name = sec.getString(key + ".username", "Player");
                int tier = sec.getInt(key + ".combined-tier", 0);
                list.add(new RankEntry(id, name, tier));
            } catch (IllegalArgumentException ignored) {
            }
        }
        list.sort(Comparator.comparingInt(RankEntry::combinedTier).reversed());
        return list.size() > maxResults ? list.subList(0, maxResults) : list;
    }

    @Override
    public synchronized void closeStore() {
        flushToFile();
    }

    private void flushToFile() {
        try {
            yaml.save(file);
        } catch (IOException ignored) {
        }
    }
}
