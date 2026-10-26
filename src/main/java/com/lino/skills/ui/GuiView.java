package com.lino.skills.ui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class GuiView implements InventoryHolder {

    private Inventory inventory;
    private final Consumer<InventoryClickEvent> clickCallback;

    public GuiView(Consumer<InventoryClickEvent> clickCallback) {
        this.clickCallback = clickCallback;
    }

    public void bind(Inventory inventory) {
        this.inventory = inventory;
    }

    public void dispatchClick(InventoryClickEvent event) {
        if (clickCallback != null) {
            clickCallback.accept(event);
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
