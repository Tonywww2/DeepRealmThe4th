package com.tony.deeprealmtheforth.astral;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Read-only location and player context passed to Java or script-defined rules. */
public final class FillerContext {
    private final Player player;
    private final ItemStack container;
    private final ItemStack filler;
    private final ContainerLayout layout;
    private final List<ItemStack> contents;
    private final int index;
    private final Set<Integer> effectiveCells;

    public FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index) {
        this(player, container, filler, layout, contents, index, openCells(layout, contents));
    }

    FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index, Set<Integer> effectiveCells) {
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
        this.effectiveCells = Set.copyOf(Objects.requireNonNull(effectiveCells, "effectiveCells"));
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
        int cell = y * layout.width() + x;
        return effectiveCells.contains(cell) ? contents.get(cell).copy() : ItemStack.EMPTY;
    }

    /** Counts only active, open cells. */
    public int count(String itemId) {
        int count = 0;
        for (int i = 0; i < contents.size(); i++) {
            if (effectiveCells.contains(i) && !contents.get(i).isEmpty()
                    && BuiltInRegistries.ITEM.getKey(contents.get(i).getItem()).toString().equals(itemId)) {
                count++;
            }
        }
        return count;
    }

    /** Counts effective items in the four orthogonal neighbors that match an item tag. */
    public int countAdjacentTag(String tagId) {
        if (tagId == null || !tagId.matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")) {
            throw new IllegalArgumentException("Expected a namespaced item tag ID, got: " + tagId);
        }
        return countAdjacentTag(TagKey.create(Registries.ITEM, ResourceLocation.tryParse(tagId)));
    }

    int countAdjacentTag(TagKey<Item> tag) {
        int x = x();
        int y = y();
        return (itemAt(x - 1, y).is(tag) ? 1 : 0)
                + (itemAt(x + 1, y).is(tag) ? 1 : 0)
                + (itemAt(x, y - 1).is(tag) ? 1 : 0)
                + (itemAt(x, y + 1).is(tag) ? 1 : 0);
    }

    private static Set<Integer> openCells(ContainerLayout layout, List<ItemStack> contents) {
        Set<Integer> cells = new HashSet<>();
        for (int i = 0; i < contents.size(); i++) {
            if (layout.isOpen(i) && !contents.get(i).isEmpty()) cells.add(i);
        }
        return cells;
    }
}
