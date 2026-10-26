package com.lino.skills.domain;

public record TierGrant(
        String targetId,
        int amountPerTier,
        int minTier,
        int maxTier
) {
    public int calculateCumulativeAt(int tier) {
        if (tier < minTier) {
            return 0;
        }
        int capped = Math.min(tier, maxTier);
        int activeTiers = (capped - minTier) + 1;
        return Math.max(0, activeTiers * amountPerTier);
    }
}
