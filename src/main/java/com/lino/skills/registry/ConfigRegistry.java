package com.lino.skills.registry;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.AttributeDefinition;
import com.lino.skills.domain.SkillDefinition;
import com.lino.skills.domain.TalentDefinition;
import com.lino.skills.domain.TierGrant;
import com.lino.skills.platform.ColorFormatter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConfigRegistry {

    private final SkillsPlugin plugin;
    private FileConfiguration messagesFile;
    private final Map<String, AttributeDefinition> attributes = new LinkedHashMap<>();
    private final Map<String, TalentDefinition> talents = new LinkedHashMap<>();
    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    public ConfigRegistry(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reloadAll() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        ensureResource("messages.yml");
        ensureResource("attributes.yml");
        ensureResource("talents.yml");
        ensureResource("skills.yml");

        messagesFile = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));

        loadAttributes();
        loadTalents();
        loadSkills();
    }

    private void ensureResource(String filename) {
        File target = new File(plugin.getDataFolder(), filename);
        if (!target.exists()) {
            plugin.saveResource(filename, false);
        }
    }

    private void loadAttributes() {
        attributes.clear();
        File file = new File(plugin.getDataFolder(), "attributes.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("attributes");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            String title = sec.getString("title", id);
            String formula = sec.getString("formula", "{level}");
            String summary = sec.getString("summary", "");
            int slot = sec.getInt("slot", 10);
            String material = sec.getString("material", "BOOK");
            attributes.put(id, new AttributeDefinition(id, title, formula, summary, slot, material));
        }
    }

    private void loadTalents() {
        talents.clear();
        File file = new File(plugin.getDataFolder(), "talents.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("talents");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            String title = sec.getString("title", id);
            String summary = sec.getString("summary", "");
            Map<String, String> formulas = new HashMap<>();
            ConfigurationSection formSec = sec.getConfigurationSection("formulas");
            if (formSec != null) {
                for (String fKey : formSec.getKeys(false)) {
                    formulas.put(fKey, formSec.getString(fKey, "0"));
                }
            }
            talents.put(id, new TalentDefinition(id, title, summary, formulas));
        }
    }

    private void loadSkills() {
        skills.clear();
        File file = new File(plugin.getDataFolder(), "skills.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        List<Long> defaultCurve = yaml.getLongList("default-xp-curve");
        if (defaultCurve.isEmpty()) {
            for (int i = 1; i <= 50; i++) {
                defaultCurve.add((long) (i * 100));
            }
        }

        ConfigurationSection root = yaml.getConfigurationSection("skills");
        if (root == null) {
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(key);
            if (sec == null) {
                continue;
            }
            String id = key.toLowerCase(Locale.ROOT);
            String title = sec.getString("title", id);
            String tagline = sec.getString("tagline", "");
            int slot = sec.getInt("slot", 20);
            String material = sec.getString("material", "BOOK");
            List<String> details = sec.getStringList("overview-details");

            List<Long> curve = sec.getLongList("xp-curve");
            if (curve.isEmpty()) {
                curve = new ArrayList<>(defaultCurve);
            }

            List<TierGrant> grants = new ArrayList<>();
            List<Map<?, ?>> grantList = sec.getMapList("grants");
            for (Map<?, ?> gMap : grantList) {
                String target = String.valueOf(gMap.get("target")).toLowerCase(Locale.ROOT);
                int amount = gMap.containsKey("amount") ? ((Number) gMap.get("amount")).intValue() : 1;
                int minTier = gMap.containsKey("min-tier") ? ((Number) gMap.get("min-tier")).intValue() : 1;
                int maxTier = gMap.containsKey("max-tier") ? ((Number) gMap.get("max-tier")).intValue() : 100;
                grants.add(new TierGrant(target, amount, minTier, maxTier));
            }

            Map<Integer, List<String>> tierNotes = new HashMap<>();
            ConfigurationSection notesSec = sec.getConfigurationSection("tier-notes");
            if (notesSec != null) {
                for (String tierKey : notesSec.getKeys(false)) {
                    try {
                        int t = Integer.parseInt(tierKey);
                        tierNotes.put(t, notesSec.getStringList(tierKey));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            Map<String, Long> blockXp = new HashMap<>();
            ConfigurationSection blockSec = sec.getConfigurationSection("block-xp");
            if (blockSec != null) {
                for (String bKey : blockSec.getKeys(false)) {
                    blockXp.put(bKey.toLowerCase(Locale.ROOT), blockSec.getLong(bKey, 1L));
                }
            }

            skills.put(id, new SkillDefinition(id, title, tagline, slot, material, details, curve, grants, tierNotes, blockXp));
        }
    }

    public String formatMessage(String key) {
        String tag = messagesFile.getString("locale.tag", "&#e879f9&lSkills &8» &r");
        String body = messagesFile.getString("locale." + key, key);
        return ColorFormatter.translateLegacy(tag + body);
    }

    public String getMenuTitle(String key, String fallback) {
        if (messagesFile == null) {
            return fallback;
        }
        return messagesFile.getString("titles." + key, fallback);
    }

    public Map<String, AttributeDefinition> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public AttributeDefinition getAttribute(String id) {
        return attributes.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, TalentDefinition> getTalents() {
        return Collections.unmodifiableMap(talents);
    }

    public TalentDefinition getTalent(String id) {
        return talents.get(id.toLowerCase(Locale.ROOT));
    }

    public Map<String, SkillDefinition> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public SkillDefinition getSkill(String id) {
        return skills.get(id.toLowerCase(Locale.ROOT));
    }
}
