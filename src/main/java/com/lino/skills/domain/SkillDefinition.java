package com.lino.skills.domain;

import java.util.List;
import java.util.Map;

public record SkillDefinition(
        String id,
        String title,
        String tagline,
        int slot,
        String material,
        List<String> overviewDetails,
        List<Long> xpThresholds,
        List<TierGrant> grants,
        Map<Integer, List<String>> tierNotes,
        Map<String, Long> blockXpTable
) {
    public int maxTier() {
        return xpThresholds.size();
    }

    public long thresholdForTier(int tier) {
        if (tier <= 0) {
            return 0L;
        }
        if (tier > xpThresholds.size()) {
            return Long.MAX_VALUE;
        }
        return xpThresholds.get(tier - 1);
    }

    public int evaluateTier(long totalAccumulatedXp) {
        long remaining = totalAccumulatedXp;
        int reached = 0;
        for (long req : xpThresholds) {
            if (remaining >= req) {
                remaining -= req;
                reached++;
            } else {
                break;
            }
        }
        return reached;
    }

    public long currentTierXp(long totalAccumulatedXp, int currentTier) {
        long consumed = 0L;
        for (int i = 0; i < currentTier && i < xpThresholds.size(); i++) {
            consumed += xpThresholds.get(i);
        }
        return Math.max(0L, totalAccumulatedXp - consumed);
    }

    public int resolveGrantAtTier(String targetId, int tier) {
        int sum = 0;
        for (TierGrant grant : grants) {
            if (grant.targetId().equalsIgnoreCase(targetId)) {
                sum += grant.calculateCumulativeAt(tier);
            }
        }
        return sum;
    }

    public List<String> resolveTierNotes(int tier) {
        int bestFloor = 1;
        for (int key : tierNotes.keySet()) {
            if (key <= tier && key >= bestFloor) {
                bestFloor = key;
            }
        }
        return tierNotes.getOrDefault(bestFloor, List.of());
    }
}
