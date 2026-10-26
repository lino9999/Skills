package com.lino.skills.persistence;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.RankEntry;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SqliteProfileStore implements ProfileStore {

    private final SkillsPlugin plugin;
    private Connection connection;

    public SqliteProfileStore(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public synchronized void initialize() {
        try {
            File dbFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.sqlite-file", "profiles.db"));
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS account_meta (" +
                        "player_id VARCHAR(36) PRIMARY KEY, " +
                        "username VARCHAR(32), " +
                        "mana DOUBLE DEFAULT 100.0, " +
                        "hud_visible BOOLEAN DEFAULT 1, " +
                        "audio_enabled BOOLEAN DEFAULT 1, " +
                        "combined_tier INTEGER DEFAULT 0)");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS account_skills (" +
                        "player_id VARCHAR(36), " +
                        "skill_id VARCHAR(32), " +
                        "tier INTEGER DEFAULT 0, " +
                        "xp BIGINT DEFAULT 0, " +
                        "PRIMARY KEY (player_id, skill_id))");

                st.executeUpdate("CREATE TABLE IF NOT EXISTS account_attributes (" +
                        "player_id VARCHAR(36), " +
                        "attr_id VARCHAR(32), " +
                        "bonus DOUBLE DEFAULT 0.0, " +
                        "PRIMARY KEY (player_id, attr_id))");
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to initialize SQLite profile store: " + ex.getMessage());
        }
    }

    @Override
    public synchronized PlayerProfile fetchProfile(UUID playerId, String username) {
        PlayerProfile profile = new PlayerProfile(playerId, username);
        String idStr = playerId.toString();

        try {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM account_meta WHERE player_id = ?")) {
                ps.setString(1, idStr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        profile.setMana(rs.getDouble("mana"));
                        profile.setHudVisible(rs.getBoolean("hud_visible"));
                        profile.setAudioEnabled(rs.getBoolean("audio_enabled"));
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("SELECT skill_id, tier, xp FROM account_skills WHERE player_id = ?")) {
                ps.setString(1, idStr);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String sId = rs.getString("skill_id");
                        profile.setSkillTier(sId, rs.getInt("tier"));
                        profile.setSkillXp(sId, rs.getLong("xp"));
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement("SELECT attr_id, bonus FROM account_attributes WHERE player_id = ?")) {
                ps.setString(1, idStr);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        profile.setBonusAttribute(rs.getString("attr_id"), rs.getDouble("bonus"));
                    }
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning("Could not load profile for " + username + ": " + ex.getMessage());
        }
        return profile;
    }

    @Override
    public synchronized void persistProfile(PlayerProfile profile) {
        String idStr = profile.getPlayerId().toString();
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO account_meta (player_id, username, mana, hud_visible, audio_enabled, combined_tier) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, idStr);
                ps.setString(2, profile.getUsername());
                ps.setDouble(3, profile.getMana());
                ps.setBoolean(4, profile.isHudVisible());
                ps.setBoolean(5, profile.isAudioEnabled());
                ps.setInt(6, profile.getCombinedTier());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO account_skills (player_id, skill_id, tier, xp) VALUES (?, ?, ?, ?)")) {
                for (String skillId : plugin.getConfigRegistry().getSkills().keySet()) {
                    ps.setString(1, idStr);
                    ps.setString(2, skillId);
                    ps.setInt(3, profile.getSkillTier(skillId));
                    ps.setLong(4, profile.getSkillXp(skillId));
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT OR REPLACE INTO account_attributes (player_id, attr_id, bonus) VALUES (?, ?, ?)")) {
                for (Map.Entry<String, Double> entry : profile.getBonusAttributesMap().entrySet()) {
                    ps.setString(1, idStr);
                    ps.setString(2, entry.getKey());
                    ps.setDouble(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException ex) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
            plugin.getLogger().warning("Could not save profile for " + profile.getUsername() + ": " + ex.getMessage());
        }
    }

    @Override
    public synchronized void deleteProfile(UUID playerId) {
        String idStr = playerId.toString();
        try {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM account_meta WHERE player_id = ?")) {
                ps.setString(1, idStr);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM account_skills WHERE player_id = ?")) {
                ps.setString(1, idStr);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM account_attributes WHERE player_id = ?")) {
                ps.setString(1, idStr);
                ps.executeUpdate();
            }
        } catch (SQLException ignored) {
        }
    }

    @Override
    public synchronized void deleteAllProfiles() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("DELETE FROM account_meta");
            st.executeUpdate("DELETE FROM account_skills");
            st.executeUpdate("DELETE FROM account_attributes");
        } catch (SQLException ignored) {
        }
    }

    @Override
    public synchronized List<RankEntry> queryTopProfiles(int maxResults) {
        List<RankEntry> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT player_id, username, combined_tier FROM account_meta ORDER BY combined_tier DESC LIMIT ?")) {
            ps.setInt(1, maxResults);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new RankEntry(
                            UUID.fromString(rs.getString("player_id")),
                            rs.getString("username"),
                            rs.getInt("combined_tier")
                    ));
                }
            }
        } catch (SQLException ignored) {
        }
        return list;
    }

    @Override
    public synchronized void closeStore() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
