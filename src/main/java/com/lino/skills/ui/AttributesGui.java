package com.lino.skills.ui;

import com.lino.skills.SkillsPlugin;
import com.lino.skills.domain.AttributeDefinition;
import com.lino.skills.domain.PlayerProfile;
import com.lino.skills.platform.ColorFormatter;
import com.lino.skills.platform.IconFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class AttributesGui {

    private AttributesGui() {
    }

    public static void display(SkillsPlugin plugin, Player player) {
        PlayerProfile profile = plugin.getProfileRepository().getProfile(player);
        if (profile == null) {
            return;
        }

        int rows = plugin.getConfig().getInt("menus.attributes.rows", 6);
        int returnSlot = plugin.getConfig().getInt("menus.attributes.return-button.slot", 48);
        int exitSlot = plugin.getConfig().getInt("menus.attributes.exit-button.slot", 49);
        int profileSlot = plugin.getConfig().getInt("menus.attributes.profile-slot", 4);
        String title = plugin.getConfigRegistry().getMenuTitle("attributes-menu", "Character Attributes");

        GuiView view = new GuiView(event -> {
            int clicked = event.getRawSlot();
            if (clicked == returnSlot) {
                OverviewGui.display(plugin, player);
                return;
            }
            if (clicked == exitSlot) {
                player.closeInventory();
            }
        });

        Inventory inv = Bukkit.createInventory(view, rows * 9, ColorFormatter.render(title));
        view.bind(inv);

        OverviewGui.paintBorders(plugin, inv, rows, "menus.attributes");

        if (profileSlot >= 0 && profileSlot < inv.getSize()) {
            inv.setItem(profileSlot, OverviewGui.createProfileHead(plugin, player, profile));
        }

        for (AttributeDefinition attr : plugin.getConfigRegistry().getAttributes().values()) {
            if (attr.slot() >= 0 && attr.slot() < inv.getSize()) {
                double val = plugin.getAttributeService().resolveAttribute(player, attr.id());
                inv.setItem(attr.slot(), createAttributeCard(plugin, attr, val));
            }
        }

        String returnMat = plugin.getConfig().getString("menus.attributes.return-button.material", "ARROW");
        String returnTitle = plugin.getConfig().getString("menus.attributes.return-button.title", "&#facc15Return to Skills");
        inv.setItem(returnSlot, IconFactory.ofMaterial(returnMat).title(returnTitle).cleanFlags().build());

        String exitMat = plugin.getConfig().getString("menus.attributes.exit-button.material", "BARRIER");
        String exitTitle = plugin.getConfig().getString("menus.attributes.exit-button.title", "&#f43f5eClose Menu");
        inv.setItem(exitSlot, IconFactory.ofMaterial(exitMat).title(exitTitle).cleanFlags().build());

        player.openInventory(inv);
    }

    private static ItemStack createAttributeCard(SkillsPlugin plugin, AttributeDefinition attr, double val) {
        String title = plugin.getConfig().getString("menus.attributes.attribute-card.title", "{attribute}")
                .replace("{attribute}", attr.title());

        int wrapLen = plugin.getConfig().getInt("menus.attributes.attribute-card.wrap-length", 30);
        List<String> rawLore = plugin.getConfig().getStringList("menus.attributes.attribute-card.lore");
        List<String> lore = new ArrayList<>();

        for (String line : rawLore) {
            String resolved = line
                    .replace("{attribute}", attr.title())
                    .replace("{value}", ColorFormatter.formatDecimal(val))
                    .replace("{summary}", attr.formatSummary(val));
            lore.addAll(ColorFormatter.wrapText(resolved, wrapLen));
        }

        return IconFactory.ofMaterial(attr.material())
                .title(title)
                .lore(lore)
                .cleanFlags()
                .build();
    }
}
