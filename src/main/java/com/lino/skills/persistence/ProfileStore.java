package com.lino.skills.persistence;

import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.RankEntry;

import java.util.List;
import java.util.UUID;

public interface ProfileStore {

    void initialize();

    PlayerProfile fetchProfile(UUID playerId, String username);

    void persistProfile(PlayerProfile profile);

    void deleteProfile(UUID playerId);

    void deleteAllProfiles();

    List<RankEntry> queryTopProfiles(int maxResults);

    void closeStore();
}
