package com.lino.skills.integration;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.AttributeDefinition;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.SkillDefinition;
import com.lino.skills.platform.ColorFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class PapiExpansion extends PlaceholderExpansion {

    private final SkillsPlugin plugin;

    public PapiExpansion(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "skills";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lino";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return "";
        }

        String token = params.toLowerCase(Locale.ROOT);
        int skillCount = plugin.getConfigRegistry().getSkills().size();

        return switch (token) {
            case "total_level", "combined_tier", "total_skill_level" -> String.valueOf(profile.getCombinedTier());
            case "average_level", "average_tier", "average_skill_level" -> ColorFormatter.formatDecimal(profile.getAverageTier(skillCount));
            case "mana" -> ColorFormatter.formatWhole((long) profile.getMana());
            case "max_mana", "mana_limit" -> ColorFormatter.formatWhole((long) plugin.getManaService().computeMaxMana(player));
            default -> evaluateDynamicPlaceholder(player, profile, token);
        };
    }

    private String evaluateDynamicPlaceholder(Player player, PlayerProfile profile, String token) {
        for (SkillDefinition skill : plugin.getConfigRegistry().getSkills().values()) {
            String prefix = skill.id() + "_";
            if (token.startsWith(prefix)) {
                String suffix = token.substring(prefix.length());
                int tier = profile.getSkillTier(skill.id());
                long curXp = skill.currentTierXp(profile.getSkillXp(skill.id()), tier);
                long nextXp = skill.thresholdForTier(tier + 1);
                return switch (suffix) {
                    case "level", "tier" -> String.valueOf(tier);
                    case "roman", "level_numeral" -> ColorFormatter.toRomanNumeral(tier);
                    case "xp" -> ColorFormatter.formatWhole(curXp);
                    case "next_xp", "required_xp" -> nextXp == Long.MAX_VALUE ? "MAX" : ColorFormatter.formatWhole(nextXp);
                    case "progress" -> nextXp > 0 && nextXp != Long.MAX_VALUE
                            ? ColorFormatter.formatDecimal(((double) curXp / nextXp) * 100.0)
                            : "100";
                    case "title", "name" -> skill.title();
                    default -> "";
                };
            }
        }

        for (AttributeDefinition attr : plugin.getConfigRegistry().getAttributes().values()) {
            if (token.equals(attr.id())) {
                return ColorFormatter.formatDecimal(plugin.getAttributeService().resolveAttribute(player, attr.id()));
            }
            if (token.equals(attr.id() + "_title") || token.equals(attr.id() + "_name")) {
                return attr.title();
            }
        }

        return null;
    }
}
