package com.lino.skills.platform;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IconFactory {

    private final ItemStack stack;
    private final ItemMeta meta;

    public IconFactory(Material material) {
        this.stack = new ItemStack(material != null ? material : Material.STONE);
        this.meta = this.stack.getItemMeta();
    }

    public static IconFactory ofMaterial(String materialName) {
        if (materialName == null || materialName.isBlank()) {
            return new IconFactory(Material.BOOK);
        }
        Material matched = Material.matchMaterial(materialName.trim().toUpperCase(Locale.ROOT));
        return new IconFactory(matched != null ? matched : Material.BOOK);
    }

    public IconFactory title(String text) {
        if (meta != null && text != null) {
            meta.displayName(ColorFormatter.render(text));
        }
        return this;
    }

    public IconFactory lore(List<String> lines) {
        if (meta != null && lines != null) {
            List<Component> rendered = new ArrayList<>(lines.size());
            for (String line : lines) {
                rendered.add(ColorFormatter.render(line));
            }
            meta.lore(rendered);
        }
        return this;
    }

    public IconFactory playerHead(String ownerName) {
        if (meta instanceof SkullMeta skullMeta && ownerName != null && !ownerName.isBlank()) {
            try {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public IconFactory amount(int count) {
        stack.setAmount(Math.max(1, Math.min(64, count)));
        return this;
    }

    public IconFactory cleanFlags() {
        if (meta != null) {
            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP,
                    ItemFlag.HIDE_UNBREAKABLE
            );
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
