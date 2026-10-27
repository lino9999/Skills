package com.lino.skills.ui;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.AttributeDefinition;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.SkillDefinition;
import com.lino.skills.domain.TalentDefinition;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.IconFactory;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class OverviewGui {

    private OverviewGui() {
    }

    public static void display(SkillsPlugin plugin, Player player) {
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return;
        }

        int rows = plugin.getConfig().getInt("menus.overview.rows", 6);
        int profileSlot = plugin.getConfig().getInt("menus.overview.profile-card.slot", 4);
        int exitSlot = plugin.getConfig().getInt("menus.overview.exit-button.slot", 49);
        String title = plugin.getConfigRegistry().getMenuTitle("overview-menu", "Skills Overview");

        GuiView view = new GuiView(event -> {
            int clicked = event.getRawSlot();
            if (clicked == profileSlot) {
                AttributesGui.display(plugin, player);
                return;
            }
            if (clicked == exitSlot) {
                player.closeInventory();
                return;
            }
            for (SkillDefinition skill : plugin.getConfigRegistry().getSkills().values()) {
                if (clicked == skill.slot()) {
                    RoadmapGui.display(plugin, player, skill, 1);
                    return;
                }
            }
        });

        Inventory inv = Bukkit.createInventory(view, rows * 9, ColorFormatter.render(title));
        view.bind(inv);

        paintBorders(plugin, inv, rows, "menus.overview");

        inv.setItem(profileSlot, createProfileHead(plugin, player, profile));

        for (SkillDefinition skill : plugin.getConfigRegistry().getSkills().values()) {
            if (skill.slot() >= 0 && skill.slot() < inv.getSize()) {
                inv.setItem(skill.slot(), createSkillCard(plugin, player, profile, skill));
            }
        }

        String exitMat = plugin.getConfig().getString("menus.overview.exit-button.material", "BARRIER");
        String exitTitle = plugin.getConfig().getString("menus.overview.exit-button.title", "&#f43f5eClose Menu");
        inv.setItem(exitSlot, IconFactory.ofMaterial(exitMat).title(exitTitle).cleanFlags().build());

        player.openInventory(inv);
    }

    public static void paintBorders(SkillsPlugin plugin, Inventory inv, int rows, String basePath) {
        ItemStack primary = IconFactory.ofMaterial(plugin.getConfig().getString(basePath + ".primary-border", "GRAY_STAINED_GLASS_PANE"))
                .title(" ")
                .cleanFlags()
                .build();
        ItemStack accent = IconFactory.ofMaterial(plugin.getConfig().getString(basePath + ".accent-border", "BLACK_STAINED_GLASS_PANE"))
                .title(" ")
                .cleanFlags()
                .build();

        List<String> layout = plugin.getConfig().getStringList(basePath + ".border-layout");
        for (int r = 0; r < layout.size() && r < rows; r++) {
            String rowStr = layout.get(r);
            for (int c = 0; c < rowStr.length() && c < 9; c++) {
                char code = rowStr.charAt(c);
                int slot = (r * 9) + c;
                if (code == 'G') {
                    inv.setItem(slot, primary);
                } else if (code == 'B') {
                    inv.setItem(slot, accent);
                }
            }
        }
    }

    public static ItemStack createProfileHead(SkillsPlugin plugin, Player player, PlayerProfile profile) {
        String title = plugin.getConfig().getString("menus.overview.profile-card.title", "&#38bdf8&l{player}'s Profile")
                .replace("{player}", player.getName());

        List<String> rawLore = plugin.getConfig().getStringList("menus.overview.profile-card.lore");
        List<String> lore = new ArrayList<>();

        int totalSkills = plugin.getConfigRegistry().getSkills().size();

        for (String line : rawLore) {
            String resolved = line;
            for (AttributeDefinition attr : plugin.getConfigRegistry().getAttributes().values()) {
                double val = plugin.getAttributeService().resolveAttribute(player, attr.id());
                double mod = profile.getBonusAttribute(attr.id());
                String modText = mod > 0 ? " &a(+" + ColorFormatter.formatDecimal(mod) + ")" : "";

                resolved = resolved
                        .replace("{attr_" + attr.id() + "_title}", attr.title())
                        .replace("{attr_" + attr.id() + "_val}", ColorFormatter.formatDecimal(val))
                        .replace("{attr_" + attr.id() + "_mod}", modText);
            }
            resolved = resolved
                    .replace("{total_level}", String.valueOf(profile.getCombinedTier()))
                    .replace("{average_level}", ColorFormatter.formatDecimal(profile.getAverageTier(totalSkills)));
            lore.add(resolved.stripTrailing());
        }

        return new IconFactory(Material.PLAYER_HEAD)
                .playerHead(player.getName())
                .title(title)
                .lore(lore)
                .cleanFlags()
                .build();
    }

    private static ItemStack createSkillCard(SkillsPlugin plugin, Player player, PlayerProfile profile, SkillDefinition skill) {
        int tier = profile.getSkillTier(skill.id());
        long totalXp = profile.getSkillXp(skill.id());
        long currentXp = skill.currentTierXp(totalXp, tier);
        long nextXp = skill.thresholdForTier(tier + 1);

        double pct = (nextXp > 0 && nextXp != Long.MAX_VALUE)
                ? Math.min(100.0, ((double) currentXp / nextXp) * 100.0)
                : 100.0;
        String meter = ColorFormatter.buildMeter(currentXp, nextXp, 10, "&a", "&7", "■");

        String title = plugin.getConfig().getString("menus.overview.skill-card.title", "&#e879f9&l{skill} &d{roman}")
                .replace("{skill}", skill.title())
                .replace("{level}", String.valueOf(tier))
                .replace("{roman}", ColorFormatter.toRomanNumeral(tier));

        int wrapLen = plugin.getConfig().getInt("menus.overview.skill-card.wrap-length", 34);
        List<String> templateLore = plugin.getConfig().getStringList("menus.overview.skill-card.lore");
        List<String> lore = new ArrayList<>();

        for (String line : templateLore) {
            if (line.trim().equals("{details}")) {
                for (String detail : skill.overviewDetails()) {
                    String parsed = resolveTokens(plugin, player, skill, -1, detail);
                    lore.addAll(ColorFormatter.wrapText(parsed, wrapLen));
                }
                continue;
            }
            String parsed = line
                    .replace("{skill}", skill.title())
                    .replace("{tagline}", skill.tagline())
                    .replace("{level}", String.valueOf(tier))
                    .replace("{roman}", ColorFormatter.toRomanNumeral(tier))
                    .replace("{percent}", ColorFormatter.formatDecimal(pct))
                    .replace("{meter}", meter)
                    .replace("{current_xp}", ColorFormatter.formatWhole(currentXp))
                    .replace("{next_xp}", nextXp == Long.MAX_VALUE ? "MAX" : ColorFormatter.formatWhole(nextXp));
            lore.add(parsed);
        }

        return IconFactory.ofMaterial(skill.material())
                .title(title)
                .lore(lore)
                .cleanFlags()
                .build();
    }

    public static String resolveTokens(SkillsPlugin plugin, Player player, SkillDefinition skill, int targetTier, String rawLine) {
        String output = rawLine;
        for (AttributeDefinition attr : plugin.getConfigRegistry().getAttributes().values()) {
            output = output.replace("{attr_" + attr.id() + "_title}", attr.title());
        }
        for (TalentDefinition talent : plugin.getConfigRegistry().getTalents().values()) {
            output = output.replace("{talent_" + talent.id() + "_title}", talent.title());

            boolean needsRoman = output.contains("{talent_" + talent.id() + "_roman}");
            boolean needsSummary = output.contains("{talent_" + talent.id() + "_summary}");
            if (needsRoman || needsSummary) {
                int lvl;
                if (targetTier > 0 && skill != null) {
                    lvl = Math.max(1, skill.resolveGrantAtTier(talent.id(), targetTier));
                } else if (player != null) {
                    lvl = Math.max(1, plugin.getAttributeService().resolveTalentLevel(player, talent.id()));
                } else {
                    lvl = 1;
                }

                if (needsRoman) {
                    output = output.replace("{talent_" + talent.id() + "_roman}", ColorFormatter.toRomanNumeral(lvl));
                }
                if (needsSummary) {
                    output = output.replace("{talent_" + talent.id() + "_summary}", talent.formatSummary(lvl));
                }
            }
        }
        return output;
    }
}
