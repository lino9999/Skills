package com.lino.skills.service;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.SkillDefinition;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.SchedulerAdapter;
import com.lino.skills.ui.OverviewGui;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressionService {

    private final SkillsPlugin plugin;
    private final Map<UUID, BossBar> playerBanners = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bannerDeadlines = new ConcurrentHashMap<>();

    public ProgressionService(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public void dismissAllBanners() {
        for (Map.Entry<UUID, BossBar> entry : playerBanners.entrySet()) {
            Player online = Bukkit.getPlayer(entry.getKey());
            if (online != null) {
                online.hideBossBar(entry.getValue());
            }
        }
        playerBanners.clear();
        bannerDeadlines.clear();
    }

    public void dismissBanner(Player player) {
        BossBar active = playerBanners.remove(player.getUniqueId());
        bannerDeadlines.remove(player.getUniqueId());
        if (active != null) {
            player.hideBossBar(active);
        }
    }

    public void awardExperience(Player player, String skillId, long rawAmount, boolean displayFeedback) {
        if (player == null || rawAmount <= 0L) {
            return;
        }
        SkillDefinition skill = plugin.getConfigRegistry().getSkill(skillId);
        if (skill == null) {
            return;
        }
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return;
        }

        double multiplier = plugin.getAttributeService().computeWisdomXpMultiplier(player);
        if (player.hasPermission("skills.boost.400")) {
            multiplier *= 4.0;
        } else if (player.hasPermission("skills.boost.300")) {
            multiplier *= 3.0;
        } else if (player.hasPermission("skills.boost.200")) {
            multiplier *= 2.0;
        } else if (player.hasPermission("skills.boost.150")) {
            multiplier *= 1.5;
        }

        long granted = Math.max(1L, Math.round(rawAmount * multiplier));
        long updatedTotal = profile.getSkillXp(skill.id()) + granted;
        profile.setSkillXp(skill.id(), updatedTotal);

        int previousTier = profile.getSkillTier(skill.id());
        int currentTier = skill.evaluateTier(updatedTotal);

        if (currentTier > previousTier) {
            processTierAdvancement(player, skill, profile, previousTier, currentTier);
        }

        if (displayFeedback) {
            dispatchXpFeedback(player, skill, profile, granted);
        }
    }

    private void dispatchXpFeedback(Player player, SkillDefinition skill, PlayerProfile profile, long granted) {
        int tier = profile.getSkillTier(skill.id());
        long progressXp = skill.currentTierXp(profile.getSkillXp(skill.id()), tier);
        long nextThreshold = skill.thresholdForTier(tier + 1);
        double pct = (nextThreshold > 0 && nextThreshold != Long.MAX_VALUE)
                ? Math.min(100.0, ((double) progressXp / nextThreshold) * 100.0)
                : 100.0;

        if (plugin.getConfig().getBoolean("notifications.xp-gain.action-overlay.enabled", true) && profile.isHudVisible()) {
            String tpl = plugin.getConfig().getString("notifications.xp-gain.action-overlay.template",
                    "&#e879f9{skill} &7({current_xp}/{next_xp}) &e+{gained} XP");
            String rendered = tpl
                    .replace("{skill}", skill.title())
                    .replace("{level}", String.valueOf(tier))
                    .replace("{roman}", ColorFormatter.toRomanNumeral(tier))
                    .replace("{current_xp}", ColorFormatter.formatWhole(progressXp))
                    .replace("{next_xp}", nextThreshold == Long.MAX_VALUE ? "MAX" : ColorFormatter.formatWhole(nextThreshold))
                    .replace("{percent}", ColorFormatter.formatDecimal(pct))
                    .replace("{gained}", ColorFormatter.formatWhole(granted));
            player.sendActionBar(ColorFormatter.render(rendered));
        }

        if (plugin.getConfig().getBoolean("notifications.xp-gain.boss-banner.enabled", true)) {
            updateBossBanner(player, skill, tier, progressXp, nextThreshold, pct, granted);
        }

        if (plugin.getConfig().getBoolean("notifications.xp-gain.audio.enabled", true) && profile.isAudioEnabled()) {
            String soundKey = plugin.getConfig().getString("notifications.xp-gain.audio.sound-id", "ENTITY_EXPERIENCE_ORB_PICKUP");
            float vol = (float) plugin.getConfig().getDouble("notifications.xp-gain.audio.volume", 0.15);
            float pitch = (float) plugin.getConfig().getDouble("notifications.xp-gain.audio.pitch", 1.8);
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundKey.toUpperCase(Locale.ROOT).replace(".", "_")), vol, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    private void updateBossBanner(Player player, SkillDefinition skill, int tier, long progressXp, long nextThreshold, double pct, long granted) {
        String tpl = plugin.getConfig().getString("notifications.xp-gain.boss-banner.template",
                "&#e879f9&l{skill} &d{roman} &8• &b{current_xp}&7/&b{next_xp} XP &8(&e+{gained}&8)");
        String text = tpl
                .replace("{skill}", skill.title())
                .replace("{level}", String.valueOf(tier))
                .replace("{roman}", ColorFormatter.toRomanNumeral(tier))
                .replace("{current_xp}", ColorFormatter.formatWhole(progressXp))
                .replace("{next_xp}", nextThreshold == Long.MAX_VALUE ? "MAX" : ColorFormatter.formatWhole(nextThreshold))
                .replace("{percent}", ColorFormatter.formatDecimal(pct))
                .replace("{gained}", ColorFormatter.formatWhole(granted));

        float fill = (float) Math.max(0.0, Math.min(1.0, pct / 100.0));
        String colorName = plugin.getConfig().getString("notifications.xp-gain.boss-banner.bar-color", "PURPLE").toUpperCase(Locale.ROOT);
        String overlayName = plugin.getConfig().getString("notifications.xp-gain.boss-banner.bar-overlay", "PROGRESS").toUpperCase(Locale.ROOT);
        long ticks = plugin.getConfig().getLong("notifications.xp-gain.boss-banner.duration-ticks", 60L);

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(colorName);
        } catch (IllegalArgumentException ex) {
            color = BossBar.Color.PURPLE;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(overlayName);
        } catch (IllegalArgumentException ex) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        UUID id = player.getUniqueId();
        BossBar banner = playerBanners.get(id);
        if (banner == null) {
            banner = BossBar.bossBar(ColorFormatter.render(text), fill, color, overlay);
            playerBanners.put(id, banner);
            player.showBossBar(banner);
        } else {
            banner.name(ColorFormatter.render(text));
            banner.progress(fill);
            banner.color(color);
            banner.overlay(overlay);
        }

        long deadline = System.currentTimeMillis() + (ticks * 50L) - 25L;
        bannerDeadlines.put(id, deadline);

        SchedulerAdapter.scheduleDelayed(plugin, () -> {
            Long scheduledDeadline = bannerDeadlines.get(id);
            if (scheduledDeadline != null && System.currentTimeMillis() >= scheduledDeadline) {
                BossBar expired = playerBanners.remove(id);
                bannerDeadlines.remove(id);
                if (expired != null) {
                    player.hideBossBar(expired);
                }
            }
        }, ticks);
    }

    private void processTierAdvancement(Player player, SkillDefinition skill, PlayerProfile profile, int oldTier, int newTier) {
        profile.setSkillTier(skill.id(), newTier);
        plugin.getAttributeService().synchronizePlayer(player);

        if (plugin.getConfig().getBoolean("notifications.tier-up.chat-announcement.enabled", true)) {
            List<String> notes = skill.resolveTierNotes(newTier);
            List<String> lines = plugin.getConfig().getStringList("notifications.tier-up.chat-announcement.lines");
            for (String line : lines) {
                if (line.trim().equals("{bonuses}")) {
                    for (String note : notes) {
                        String resolved = OverviewGui.resolveTokens(plugin, player, skill, newTier, note);
                        player.sendMessage(ColorFormatter.render(resolved));
                    }
                    continue;
                }
                String parsed = line
                        .replace("{skill}", skill.title())
                        .replace("{level}", String.valueOf(newTier))
                        .replace("{roman}", ColorFormatter.toRomanNumeral(newTier));
                player.sendMessage(ColorFormatter.render(parsed));
            }
        }

        if (plugin.getConfig().getBoolean("notifications.tier-up.screen-title.enabled", true)) {
            String header = plugin.getConfig().getString("notifications.tier-up.screen-title.header", "&#e879f9&l{skill} &fTier Up!")
                    .replace("{skill}", skill.title())
                    .replace("{level}", String.valueOf(newTier))
                    .replace("{old_level}", String.valueOf(oldTier));
            String sub = plugin.getConfig().getString("notifications.tier-up.screen-title.subheader", "&7Tier {old_level} &8➔ &aTier {level}")
                    .replace("{skill}", skill.title())
                    .replace("{level}", String.valueOf(newTier))
                    .replace("{old_level}", String.valueOf(oldTier));

            player.showTitle(Title.title(
                    ColorFormatter.render(header),
                    ColorFormatter.render(sub),
                    Title.Times.times(Duration.ofMillis(400), Duration.ofMillis(2200), Duration.ofMillis(400))
            ));
        }

        if (plugin.getConfig().getBoolean("notifications.tier-up.audio.enabled", true)) {
            String soundKey = plugin.getConfig().getString("notifications.tier-up.audio.sound-id", "ENTITY_PLAYER_LEVELUP");
            float vol = (float) plugin.getConfig().getDouble("notifications.tier-up.audio.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("notifications.tier-up.audio.pitch", 1.0);
            try {
                player.playSound(player.getLocation(), Sound.valueOf(soundKey.toUpperCase(Locale.ROOT).replace(".", "_")), vol, pitch);
            } catch (Exception ignored) {
            }
        }

        if (plugin.getConfig().getBoolean("notifications.tier-up.launch-firework", true)) {
            SchedulerAdapter.executeAtEntity(plugin, player, () -> {
                Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.fromRGB(232, 121, 249), Color.fromRGB(56, 189, 248))
                        .withFade(Color.WHITE)
                        .trail(true)
                        .build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            });
        }
    }
}
