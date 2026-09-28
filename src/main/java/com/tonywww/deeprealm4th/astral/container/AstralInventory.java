package com.tonywww.deeprealm4th.astral.container;

import com.tonywww.deeprealm4th.item.AstralContainerItem;
import com.tonywww.deeprealm4th.astral.AstralFillers;
import com.tonywww.deeprealm4th.platform.data.AstralStoredItems;
import com.tonywww.deeprealm4th.astral.FillerActivation;
import com.tonywww.deeprealm4th.astral.FillerDefinition;

import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A menu-facing view of the cells stored on one concrete container ItemStack. */
public final class AstralInventory implements Container {
    private final ItemStack owner;
    private final ContainerLayout layout;
    private final Predicate<Player> stillOwned;
    private final NonNullList<ItemStack> items;

    public AstralInventory(ItemStack owner, ContainerLayout layout, Predicate<Player> stillOwned) {
        if (owner.isEmpty()) throw new IllegalArgumentException("Container ItemStack is empty");
        this.owner = owner;
        this.layout = layout;
        this.stillOwned = stillOwned;
        this.items = AstralStoredItems.read(owner, layout.size());
    }

    public ContainerLayout layout() {
        return layout;
    }

    public ItemStack owner() {
        return owner;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : items) {
            if (!item.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, index, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (index < 0 || index >= items.size()) return ItemStack.EMPTY;
        ItemStack removed = items.set(index, ItemStack.EMPTY);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index < 0 || index >= items.size()) throw new IndexOutOfBoundsException(index);
        // The client also receives old contents of cells closed by a config change.
        // Insertion is enforced by the menu Slot and this container's mayPlace rule.
        items.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        if (!layout.isOpen(index) || stack.isEmpty()
                || !(owner.getItem() instanceof AstralContainerItem container)
                || !container.mayInsert(this, index, stack)) return false;
        FillerDefinition definition = AstralFillers.find(stack);
        if (definition == null) return false;
        if (definition.activation() == FillerActivation.UNIQUE_WORN) {
            for (int i = 0; i < items.size(); i++) {
                if (i != index && !items.get(i).isEmpty() && items.get(i).is(stack.getItem())) return false;
            }
        }
        return true;
    }

    @Override
    public void setChanged() {
        AstralStoredItems.write(owner, items);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && stillOwned.test(player);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < items.size(); i++) items.set(i, ItemStack.EMPTY);
        setChanged();
    }
}
