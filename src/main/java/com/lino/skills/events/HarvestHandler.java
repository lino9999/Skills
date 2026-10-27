package com.lino.skills.events;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.SkillDefinition;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class HarvestHandler implements Listener {

    private static final String ARTIFICIAL_TAG = "skills_artificial_block";
    private final SkillsPlugin plugin;

    public HarvestHandler(SkillsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        event.getBlock().setMetadata(ARTIFICIAL_TAG, new FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }
        List<String> blacklisted = plugin.getConfig().getStringList("blacklisted-worlds");
        if (blacklisted.contains(player.getWorld().getName())) {
            return;
        }

        Block block = event.getBlock();
        boolean artificial = block.hasMetadata(ARTIFICIAL_TAG);
        if (block.getBlockData() instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) {
            artificial = false;
        }

        String key = block.getType().name().toLowerCase(Locale.ROOT);

        evaluateBlockExperience(player, block, key, artificial);
        evaluateBonusYields(player, block, artificial);
        evaluateDynamicMining(player);

        block.removeMetadata(ARTIFICIAL_TAG, plugin);
    }

    private void evaluateBlockExperience(Player player, Block block, String key, boolean artificial) {
        for (SkillDefinition skill : plugin.getConfigRegistry().getSkills().values()) {
            Long baseXp = skill.blockXpTable().get(key);
            if (baseXp == null || baseXp <= 0L) {
                continue;
            }

            int stackCount = 1;
            if (isVerticalPlant(block.getType())) {
                stackCount = artificial ? 0 : 1;
                Block above = block.getRelative(BlockFace.UP);
                while (above.getType() == block.getType() && stackCount < 16) {
                    if (!above.hasMetadata(ARTIFICIAL_TAG)) {
                        stackCount++;
                    }
                    above = above.getRelative(BlockFace.UP);
                }
                if (stackCount <= 0) {
                    return;
                }
            } else if (artificial) {
                return;
            }

            if (skill.id().equals("farming") && block.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    return;
                }
            }

            plugin.getProgressionService().awardExperience(player, skill.id(), baseXp * stackCount, true);
            return;
        }
    }

    private void evaluateBonusYields(Player player, Block block, boolean artificial) {
        if (artificial && !isVerticalPlant(block.getType())) {
            return;
        }
        Material type = block.getType();

        int spelunking = plugin.getAttributeService().resolveTalentLevel(player, "spelunking");
        if (spelunking > 0 && isMineralVein(type)) {
            rollExtraDrops(block, spelunking);
        }

        int lumberjack = plugin.getAttributeService().resolveTalentLevel(player, "master_lumberjack");
        if (lumberjack > 0 && isTimberBlock(type)) {
            rollExtraDrops(block, lumberjack);
        }

        if (isHarvestableCrop(block)) {
            int bountiful = plugin.getAttributeService().resolveTalentLevel(player, "bountiful_harvest");
            if (bountiful > 0) {
                rollExtraDrops(block, bountiful);
            }

            int goldenYield = plugin.getAttributeService().resolveTalentLevel(player, "golden_yield");
            if (goldenYield > 0 && Math.random() * 100.0 < Math.min(100.0, goldenYield * 0.5)) {
                spawnAdditionalDrops(block, 5);
            }
        }
    }

    private void rollExtraDrops(Block block, int talentLevel) {
        int rollChance = (talentLevel * 8) % 100;
        int totalCopies = (int) Math.floor((talentLevel * 8.0) / 100.0) + 1;
        if (Math.random() * 100.0 < rollChance) {
            totalCopies++;
        }
        if (totalCopies > 1) {
            spawnAdditionalDrops(block, totalCopies - 1);
        }
    }

    private void spawnAdditionalDrops(Block block, int copies) {
        Collection<ItemStack> naturalDrops = block.getDrops();
        for (int i = 0; i < copies; i++) {
            for (ItemStack item : naturalDrops) {
                block.getWorld().dropItemNaturally(block.getLocation(), item.clone());
            }
        }
    }

    private void evaluateDynamicMining(Player player) {
        int lvl = plugin.getAttributeService().resolveTalentLevel(player, "dynamic_mining");
        if (lvl > 0 && Math.random() * 100.0 < (lvl * 0.5)) {
            int ticks = 18 + (lvl * 2);
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, ticks, 2));
        }
    }

    private boolean isMineralVein(Material mat) {
        String name = mat.name();
        return name.endsWith("_ORE") || name.equals("ANCIENT_DEBRIS");
    }

    private boolean isTimberBlock(Material mat) {
        String name = mat.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM");
    }

    private boolean isVerticalPlant(Material mat) {
        return mat == Material.SUGAR_CANE || mat == Material.CACTUS || mat == Material.BAMBOO;
    }

    private boolean isHarvestableCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        Material mat = block.getType();
        return isVerticalPlant(mat) || mat == Material.MELON || mat == Material.PUMPKIN;
    }
}
