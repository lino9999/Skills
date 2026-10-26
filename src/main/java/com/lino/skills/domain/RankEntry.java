package com.lino.skills.domain;

import java.util.UUID;

public record RankEntry(UUID playerId, String username, int combinedTier) {
}
