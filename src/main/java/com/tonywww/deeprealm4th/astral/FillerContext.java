package com.tonywww.deeprealm4th.astral;

import com.tonywww.deeprealm4th.astral.container.ContainerLayout;
import com.tonywww.deeprealm4th.astral.node.CellView;
import com.tonywww.deeprealm4th.astral.node.FillerFilter;
import com.tonywww.deeprealm4th.astral.node.FillerResult;
import com.tonywww.deeprealm4th.astral.node.PlayerStateSnapshot;
import com.tonywww.deeprealm4th.astral.node.ReadPlan;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
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
    private final List<CellView> cellViews;
    private final PlayerStateSnapshot state;
    private final Map<Integer, FillerResult> baseResults, finalResults;
    private final Set<Integer> allowedFinal;
    private final ReadPlan.Inputs inputs;

    public FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index) {
        this(player, container, filler, layout, contents, index, openCells(layout, contents));
    }

    public FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index, PlayerStateSnapshot state) {
        this(player, container, filler, layout, contents, index, openCells(layout, contents),
                defaultViews(layout, contents, openCells(layout, contents)), state,
                Map.of(), Map.of(), Set.of(), new ReadPlan.Inputs(Map.of(), Map.of()));
    }

    public FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index, Set<Integer> effectiveCells) {
        this(player, container, filler, layout, contents, index, effectiveCells,
                defaultViews(layout, contents, effectiveCells), PlayerStateSnapshot.capture(player),
                Map.of(), Map.of(), Set.of(), new ReadPlan.Inputs(Map.of(), Map.of()));
    }

    public FillerContext(Player player, ItemStack container, ItemStack filler,
            ContainerLayout layout, List<ItemStack> contents, int index, Set<Integer> effectiveCells,
            List<CellView> views, PlayerStateSnapshot state, Map<Integer, FillerResult> bases,
            Map<Integer, FillerResult> finals, Set<Integer> allowedFinal, ReadPlan.Inputs inputs) {
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
        if (views.size() != layout.size()) throw new IllegalArgumentException("Cell views do not match layout");
        this.cellViews = List.copyOf(views);
        this.state = Objects.requireNonNull(state, "state");
        this.baseResults = Map.copyOf(bases);
        this.finalResults = Map.copyOf(finals);
        this.allowedFinal = Set.copyOf(allowedFinal);
        this.inputs = Objects.requireNonNull(inputs, "inputs");
    }

    public Player player() { return player; }
    public ItemStack container() { return container.copy(); }
    public ItemStack filler() { return filler.copy(); }
    public ContainerLayout layout() { return layout; }
    public int index() { return index; }
    public int x() { return index % layout.width(); }
    public int y() { return index / layout.width(); }
    public PlayerStateSnapshot state() { return state; }
    public List<CellView> cells() { return cellViews; }
    public CellView cellAt(int x, int y) {
        return x < 0 || y < 0 || x >= layout.width() || y >= layout.height()
                ? CellView.outOfBounds(x, y) : cellViews.get(y * layout.width() + x);
    }
    public CellView offset(int dx, int dy) { return cellAt(x() + dx, y() + dy); }
    public List<CellView> clockwiseNeighbors() {
        return List.of(offset(0, -1), offset(1, 0), offset(0, 1), offset(-1, 0));
    }
    public List<CellView> orthogonal() { return clockwiseNeighbors(); }
    public List<CellView> diagonal() {
        return List.of(offset(-1, -1), offset(1, -1), offset(1, 1), offset(-1, 1));
    }
    public record CellPair(CellView first, CellView second) {}
    public List<CellPair> opposedPairs() {
        return List.of(new CellPair(offset(-1, 0), offset(1, 0)),
                new CellPair(offset(0, -1), offset(0, 1)));
    }
    public CellView mirrorCell() {
        return cellAt(layout.width() - 1 - x(), layout.height() - 1 - y());
    }
    public List<List<CellView>> squaresContainingSelf(int size) {
        if (size < 1 || size > Math.max(layout.width(), layout.height()))
            throw new IllegalArgumentException("Invalid square size");
        List<List<CellView>> squares = new ArrayList<>();
        for (int top = y() - size + 1; top <= y(); top++) {
            for (int left = x() - size + 1; left <= x(); left++) {
                if (left < 0 || top < 0 || left + size > layout.width() || top + size > layout.height()) continue;
                List<CellView> square = new ArrayList<>(size * size);
                for (int row = top; row < top + size; row++)
                    for (int col = left; col < left + size; col++) square.add(cellAt(col, row));
                squares.add(List.copyOf(square));
            }
        }
        return List.copyOf(squares);
    }
    public enum Direction { UP, RIGHT, DOWN, LEFT }
    public List<CellView> ray(Direction direction, java.util.function.Predicate<CellView> continueWhile) {
        int dx = switch (direction) { case RIGHT -> 1; case LEFT -> -1; default -> 0; };
        int dy = switch (direction) { case DOWN -> 1; case UP -> -1; default -> 0; };
        List<CellView> result = new ArrayList<>();
        for (int distance = 1; ; distance++) {
            CellView cell = offset(dx * distance, dy * distance);
            if (!cell.inBounds() || !continueWhile.test(cell)) break;
            result.add(cell);
        }
        return List.copyOf(result);
    }
    public List<CellView> connected(FillerFilter filter) {
        CellView start = cellAt(x(), y());
        if (!filter.test(start)) return List.of();
        Set<Integer> seen = new HashSet<>();
        ArrayDeque<CellView> queue = new ArrayDeque<>();
        List<CellView> result = new ArrayList<>();
        seen.add(start.index());
        queue.add(start);
        while (!queue.isEmpty()) {
            CellView current = queue.removeFirst();
            result.add(current);
            for (int[] delta : new int[][]{{0,-1},{1,0},{0,1},{-1,0}}) {
                CellView neighbor = cellAt(current.x() + delta[0], current.y() + delta[1]);
                if (neighbor.inBounds() && seen.add(neighbor.index()) && filter.test(neighbor)) queue.add(neighbor);
            }
        }
        return List.copyOf(result);
    }
    public long count(FillerFilter filter) { return cellViews.stream().filter(filter).count(); }
    public FillerResult baseAt(CellView cell) {
        return baseResults.getOrDefault(cell.index(), FillerResult.unavailable(cell.index(), "base_unavailable"));
    }
    public FillerResult resultAt(CellView cell) {
        if (!allowedFinal.contains(cell.index())) return FillerResult.unavailable(cell.index(), "undeclared_final_read");
        return finalResults.getOrDefault(cell.index(), FillerResult.unavailable(cell.index(), "final_unavailable"));
    }
    public ReadPlan.Inputs inputs() { return inputs; }

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

    private static List<CellView> defaultViews(ContainerLayout layout, List<ItemStack> contents, Set<Integer> active) {
        List<CellView> views = new ArrayList<>(contents.size());
        for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            views.add(new CellView(i, i % layout.width(), i / layout.width(), true, layout.isOpen(i),
                    active.contains(i), layout.isOpen(i) ? "" : "closed",
                    stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    "", "", "", "", stack, null));
        }
        return List.copyOf(views);
    }
}
