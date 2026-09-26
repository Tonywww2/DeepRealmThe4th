package com.tony.deeprealmtheforth.astral;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Read-only location and player context passed to Java or script-defined rules. */
public final class FillerContext {
    private final Player player;
    private final ItemStack container;
    private final ItemStack filler;
    private final ContainerLayout layout;
    private final List<ItemStack> contents;
    private final int index;

    public FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index) {
        this.player = Objects.requireNonNull(player, "player");
        this.container = Objects.requireNonNull(container, "container").copy();
        this.filler = Objects.requireNonNull(filler, "filler").copy();
        this.layout = Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(contents, "contents");
        if (contents.size() != layout.size() || index < 0 || index >= layout.size()) {
            throw new IllegalArgumentException("Filler context does not match the container layout");
        }
        // The list is not exposed; itemAt returns copies. One calculation can share its snapshot.
        this.contents = List.copyOf(contents);
        this.index = index;
    }

    public Player player() { return player; }
    public ItemStack container() { return container.copy(); }
    public ItemStack filler() { return filler.copy(); }
    public ContainerLayout layout() { return layout; }
    public int index() { return index; }
    public int x() { return index % layout.width(); }
    public int y() { return index / layout.width(); }

    public ItemStack itemAt(int x, int y) {
        if (!layout.isOpen(x, y)) return ItemStack.EMPTY;
        return contents.get(y * layout.width() + x).copy();
    }

    /** Counts only active, open cells. */
    public int count(String itemId) {
        int count = 0;
        for (int i = 0; i < contents.size(); i++) {
            if (layout.isOpen(i) && !contents.get(i).isEmpty()
                    && BuiltInRegistries.ITEM.getKey(contents.get(i).getItem()).toString().equals(itemId)) {
                count++;
            }
        }
        return count;
    }
}
