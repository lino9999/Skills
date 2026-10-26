package com.lino.skills.service;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.RankEntry;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RankingService {

    private final SkillsPlugin plugin;
    private final List<RankEntry> cachedRankings = new CopyOnWriteArrayList<>();

    public RankingService(SkillsPlugin plugin) {
        this.plugin = plugin;
        int refreshSecs = plugin.getConfig().getInt("ranking.refresh-seconds", 60);
        SchedulerAdapter.scheduleAsyncRepeating(plugin, this::refreshCache, 40L, refreshSecs * 20L);
    }

    public void refreshCache() {
        int limit = plugin.getConfig().getInt("ranking.display-limit", 10);
        List<RankEntry> fresh = plugin.getProfileRepository().fetchRankings(limit);
        cachedRankings.clear();
        cachedRankings.addAll(fresh);
    }

    public void displayRankings(CommandSender viewer) {
        viewer.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("ranking-banner")));
        if (cachedRankings.isEmpty()) {
            viewer.sendMessage(ColorFormatter.render(plugin.getConfigRegistry().formatMessage("ranking-empty")));
            return;
        }
        String rowTpl = plugin.getConfigRegistry().formatMessage("ranking-row");
        int rank = 1;
        for (RankEntry entry : cachedRankings) {
            String line = rowTpl
                    .replace("{pos}", String.valueOf(rank++))
                    .replace("{player}", entry.username())
                    .replace("{total}", String.valueOf(entry.combinedTier()));
            viewer.sendMessage(ColorFormatter.render(line));
        }
    }
}
