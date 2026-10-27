package com.lino.skills.ui;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.domain.SkillDefinition;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.IconFactory;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class RoadmapGui {

    private RoadmapGui() {
    }

    public static void display(SkillsPlugin plugin, Player player, SkillDefinition skill, int page) {
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return;
        }

        List<Integer> pathSlots = plugin.getConfig().getIntegerList("menus.roadmap.path-slots");
        if (pathSlots.isEmpty()) {
            pathSlots = List.of(0, 9, 18, 27, 28, 29, 20, 11, 2, 3, 4, 13, 22, 31, 32, 33, 24, 15, 6, 7, 8, 17, 26, 35, 44, 53);
        }

        int nodesPerPage = pathSlots.size();
        int maxPage = Math.max(1, (int) Math.ceil((double) skill.maxTier() / nodesPerPage));
        int safePage = Math.max(1, Math.min(page, maxPage));

        int rows = plugin.getConfig().getInt("menus.roadmap.rows", 6);
        int returnSlot = plugin.getConfig().getInt("menus.roadmap.return-button.slot", 45);
        int prevSlot = plugin.getConfig().getInt("menus.roadmap.prev-button.slot", 48);
        int exitSlot = plugin.getConfig().getInt("menus.roadmap.exit-button.slot", 49);
        int nextSlot = plugin.getConfig().getInt("menus.roadmap.next-button.slot", 50);

        String titleTpl = plugin.getConfigRegistry().getMenuTitle("roadmap-menu", "{skill} Roadmap ({page}/{max_page})");
        String title = titleTpl
                .replace("{skill}", skill.title())
                .replace("{page}", String.valueOf(safePage))
                .replace("{max_page}", String.valueOf(maxPage));

        GuiView view = new GuiView(event -> {
            int clicked = event.getRawSlot();
            if (clicked == returnSlot) {
                OverviewGui.display(plugin, player);
                return;
            }
            if (clicked == exitSlot) {
                player.closeInventory();
                return;
            }
            if (clicked == prevSlot && safePage > 1) {
                playClickSound(player);
                display(plugin, player, skill, safePage - 1);
                return;
            }
            if (clicked == nextSlot && safePage < maxPage) {
                playClickSound(player);
                display(plugin, player, skill, safePage + 1);
            }
        });

        Inventory inv = Bukkit.createInventory(view, rows * 9, ColorFormatter.render(title));
        view.bind(inv);

        ItemStack backdrop = IconFactory.ofMaterial(plugin.getConfig().getString("menus.roadmap.background-pane", "BLACK_STAINED_GLASS_PANE"))
                .title(" ")
                .cleanFlags()
                .build();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, backdrop);
        }

        int playerTier = profile.getSkillTier(skill.id());
        long currentXp = skill.currentTierXp(profile.getSkillXp(skill.id()), playerTier);
        int startTier = ((safePage - 1) * nodesPerPage) + 1;

        for (int idx = 0; idx < nodesPerPage; idx++) {
            int tier = startTier + idx;
            if (tier > skill.maxTier()) {
                break;
            }
            int slot = pathSlots.get(idx);
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, createNodeIcon(plugin, player, skill, tier, playerTier, currentXp));
            }
        }

        inv.setItem(returnSlot, IconFactory.ofMaterial(plugin.getConfig().getString("menus.roadmap.return-button.material", "OAK_DOOR"))
                .title(plugin.getConfig().getString("menus.roadmap.return-button.title", "&#facc15Return to Overview"))
                .cleanFlags()
                .build());

        if (safePage > 1) {
            inv.setItem(prevSlot, IconFactory.ofMaterial(plugin.getConfig().getString("menus.roadmap.prev-button.material", "ARROW"))
                    .title(plugin.getConfig().getString("menus.roadmap.prev-button.title", "&fPrevious Page"))
                    .cleanFlags()
                    .build());
        }

        if (safePage < maxPage) {
            inv.setItem(nextSlot, IconFactory.ofMaterial(plugin.getConfig().getString("menus.roadmap.next-button.material", "ARROW"))
                    .title(plugin.getConfig().getString("menus.roadmap.next-button.title", "&fNext Page"))
                    .cleanFlags()
                    .build());
        } else {
            inv.setItem(nextSlot, IconFactory.ofMaterial(plugin.getConfig().getString("menus.roadmap.next-button.inactive-material", "GRAY_DYE"))
                    .title("&7Max Page")
                    .cleanFlags()
                    .build());
        }

        inv.setItem(exitSlot, IconFactory.ofMaterial(plugin.getConfig().getString("menus.roadmap.exit-button.material", "BARRIER"))
                .title(plugin.getConfig().getString("menus.roadmap.exit-button.title", "&#f43f5eClose Menu"))
                .cleanFlags()
                .build());

        player.openInventory(inv);
    }

    private static ItemStack createNodeIcon(SkillsPlugin plugin, Player player, SkillDefinition skill, int tier, int playerTier, long currentXp) {
        String stateNode;
        if (tier <= playerTier) {
            stateNode = "completed-node";
        } else if (tier == playerTier + 1) {
            stateNode = "active-node";
        } else {
            stateNode = "locked-node";
        }

        String basePath = "menus.roadmap." + stateNode;
        String mat = plugin.getConfig().getString(basePath + ".material", "RED_STAINED_GLASS_PANE");
        String title = plugin.getConfig().getString(basePath + ".title", "{skill} Tier {roman}")
                .replace("{skill}", skill.title())
                .replace("{level}", String.valueOf(tier))
                .replace("{roman}", ColorFormatter.toRomanNumeral(tier));

        long nextXp = skill.thresholdForTier(tier);
        double pct;
        if (tier <= playerTier) {
            pct = 100.0;
        } else if (tier == playerTier + 1) {
            pct = nextXp > 0 ? Math.min(100.0, ((double) currentXp / nextXp) * 100.0) : 100.0;
        } else {
            pct = 0.0;
        }

        List<String> tierNotes = skill.resolveTierNotes(tier);
        List<String> rawLore = plugin.getConfig().getStringList(basePath + ".lore");
        List<String> lore = new ArrayList<>();

        for (String line : rawLore) {
            if (line.trim().equals("{bonuses}")) {
                for (String note : tierNotes) {
                    String resolved = OverviewGui.resolveTokens(plugin, player, skill, tier, note);
                    lore.addAll(ColorFormatter.wrapText(resolved, 36));
                }
                continue;
            }
            String resolved = line
                    .replace("{skill}", skill.title())
                    .replace("{level}", String.valueOf(tier))
                    .replace("{roman}", ColorFormatter.toRomanNumeral(tier))
                    .replace("{percent}", ColorFormatter.formatDecimal(pct))
                    .replace("{current_xp}", ColorFormatter.formatWhole(currentXp))
                    .replace("{next_xp}", ColorFormatter.formatWhole(nextXp));
            lore.add(resolved);
        }

        return IconFactory.ofMaterial(mat)
                .amount(Math.min(64, tier))
                .title(title)
                .lore(lore)
                .cleanFlags()
                .build();
    }

    private static void playClickSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }
}
