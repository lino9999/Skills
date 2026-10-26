package com.lino.skills.persistence;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.RankEntry;
import com.lino.skills.platform.SchedulerAdapter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProfileRepository {

    private final SkillsPlugin plugin;
    private final ProfileStore store;
    private final Map<UUID, PlayerProfile> activeProfiles = new ConcurrentHashMap<>();

    public ProfileRepository(SkillsPlugin plugin) {
        this.plugin = plugin;
        String driver = plugin.getConfig().getString("storage.driver", "SQLITE");
        if (driver.equalsIgnoreCase("YAML")) {
            this.store = new YamlProfileStore(plugin);
        } else {
            this.store = new SqliteProfileStore(plugin);
        }
        this.store.initialize();

        int autosaveSecs = plugin.getConfig().getInt("storage.autosave-seconds", 300);
        SchedulerAdapter.scheduleAsyncRepeating(plugin, this::saveAllCached, autosaveSecs * 20L, autosaveSecs * 20L);
    }

    public void loadProfileAsync(Player player) {
        UUID id = player.getUniqueId();
        String name = player.getName();
        SchedulerAdapter.executeAsync(plugin, () -> {
            PlayerProfile profile = store.fetchProfile(id, name);
            activeProfiles.put(id, profile);
            SchedulerAdapter.executeAtEntity(plugin, player, () -> {
                if (player.isOnline()) {
                    plugin.getAttributeService().synchronizePlayer(player);
                    profile.setMana(plugin.getManaService().computeMaxMana(player));
                }
            });
        });
    }

    public void releaseProfile(UUID playerId) {
        PlayerProfile removed = activeProfiles.remove(playerId);
        if (removed != null) {
            SchedulerAdapter.executeAsync(plugin, () -> store.persistProfile(removed));
        }
    }

    public PlayerProfile getProfile(Player player) {
        return activeProfiles.computeIfAbsent(player.getUniqueId(), id -> new PlayerProfile(id, player.getName()));
    }

    public PlayerProfile getProfile(UUID playerId) {
        return activeProfiles.get(playerId);
    }

    public void saveAllCached() {
        for (PlayerProfile profile : activeProfiles.values()) {
            store.persistProfile(profile);
        }
    }

    public void wipeProfile(UUID playerId) {
        activeProfiles.remove(playerId);
        SchedulerAdapter.executeAsync(plugin, () -> store.deleteProfile(playerId));
    }

    public void wipeAllProfiles() {
        activeProfiles.clear();
        SchedulerAdapter.executeAsync(plugin, store::deleteAllProfiles);
    }

    public List<RankEntry> fetchRankings(int limit) {
        saveAllCached();
        return store.queryTopProfiles(limit);
    }

    public void shutdown() {
        saveAllCached();
        store.closeStore();
    }
}
