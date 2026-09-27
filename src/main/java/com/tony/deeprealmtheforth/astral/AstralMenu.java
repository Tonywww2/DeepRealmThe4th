package com.tony.deeprealmtheforth.astral;

import com.tony.deeprealmtheforth.platform.items.AstralItemRegistration;
import com.tony.deeprealmtheforth.platform.items.AstralMenuRegistration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A menu bound to the exact ItemStack that was opened, rather than to an item type. */
public final class AstralMenu extends AbstractContainerMenu {
    private final AstralInventory cells;
    private final int ownerSlot;
    private final int cellCount;
    private final int playerSlotsStart;
    private final Player viewer;
    private final SimpleContainerData scoreData = new SimpleContainerData(ScoreType.values().length * 4);
    private final SimpleContainerData inactiveData;

    public AstralMenu(int id, Inventory playerInventory, int ownerSlot, ItemStack owner, ContainerLayout layout) {
        super(AstralMenuRegistration.MENU.get(), id);
        this.ownerSlot = ownerSlot;
        this.viewer = playerInventory.player;
        this.cells = new AstralInventory(owner, layout,
                player -> player.level().isClientSide
                        || player.getInventory().getItem(ownerSlot) == owner);
        this.cellCount = layout.size();
        this.inactiveData = new SimpleContainerData(cellCount);
        this.playerSlotsStart = cellCount;
        int bodyY = 18 + Math.max(layout.height() * 18, 108);
        int playerX = Math.max(8, (screenWidth(layout) - 162) / 2);
        int gridX = gridX(layout);

        for (int y = 0; y < layout.height(); y++) {
            for (int x = 0; x < layout.width(); x++) {
                int index = y * layout.width() + x;
                addSlot(new Slot(cells, index, gridX + x * 18, 18 + y * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return cells.canPlaceItem(index, stack);
                    }

                    @Override
                    public int getMaxStackSize() {
                        return 1;
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + (row + 1) * 9;
                addSlot(playerSlot(playerInventory, index,
                        playerX + col * 18, bodyY + 16 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(playerSlot(playerInventory, col, playerX + col * 18, bodyY + 74));
        }
        addDataSlots(scoreData);
        addDataSlots(inactiveData);
    }

    public static int screenWidth(ContainerLayout layout) {
        return Math.max(176, layout.width() * 18 + 16);
    }

    public static int gridX(ContainerLayout layout) {
        return (screenWidth(layout) - layout.width() * 18) / 2;
    }

    private Slot playerSlot(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return index != ownerSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return index != ownerSlot;
            }
        };
    }

    public static AstralMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        int ownerSlot = buffer.readVarInt();
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        List<String> rows = new ArrayList<>(height);
        for (int i = 0; i < height; i++) rows.add(buffer.readUtf(width));
        ContainerLayout layout = ContainerLayout.parse(width, height, rows);
        ItemStack owner = inventory.getItem(ownerSlot);
        // The inventory packet can precede the item sync on a newly joined client.
        if (owner.isEmpty()) owner = new ItemStack(AstralItemRegistration.BASE_CONTAINER.get());
        return new AstralMenu(id, inventory, ownerSlot, owner, layout);
    }

    public static void writeOpeningData(FriendlyByteBuf buffer, int ownerSlot, ContainerLayout layout) {
        buffer.writeVarInt(ownerSlot);
        buffer.writeVarInt(layout.width());
        buffer.writeVarInt(layout.height());
        for (String row : layout.rows()) buffer.writeUtf(row);
    }

    public ContainerLayout layout() {
        return cells.layout();
    }

    public double score(ScoreType type) {
        long bits = 0;
        for (int i = 0; i < 4; i++) {
            bits |= ((long) scoreData.get(type.ordinal() * 4 + i) & 0xffffL) << (i * 16);
        }
        return Double.longBitsToDouble(bits);
    }

    /** 0 means active, 1 means suppressed, and 2 means duplicate. */
    public int inactiveReason(int cell) {
        return cell >= 0 && cell < cellCount ? inactiveData.get(cell) : 0;
    }

    @Override
    public void broadcastChanges() {
        if (!viewer.level().isClientSide) {
            List<ItemStack> contents = new ArrayList<>(cellCount);
            for (int i = 0; i < cellCount; i++) contents.add(cells.getItem(i));
            AstralScoreEngine.Result result = AstralScoreEngine.calculate(viewer, cells.owner(), layout(), contents);
            ScoreSheet scores = result.scores();
            for (ScoreType type : ScoreType.values()) {
                long bits = Double.doubleToRawLongBits(scores.get(type));
                for (int i = 0; i < 4; i++) {
                    scoreData.set(type.ordinal() * 4 + i, (int) ((bits >>> (i * 16)) & 0xffff));
                }
            }
            for (int i = 0; i < cellCount; i++) {
                AstralScoreEngine.InactiveReason reason = result.inactiveReasons().get(i);
                inactiveData.set(i, reason == null ? 0 : reason.ordinal() + 1);
            }
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        return cells.stillValid(player);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // The owner cannot be removed while its menu is open. Inventory identity is
        // also checked on every tick in case another mod moves it.
        if (slotId >= playerSlotsStart && slotId < slots.size()
                && slots.get(slotId).container == player.getInventory()
                && slots.get(slotId).getContainerSlot() == ownerSlot) return;
        if (clickType == ClickType.SWAP && button == ownerSlot) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (slot.container == player.getInventory() && slot.getContainerSlot() == ownerSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack copy = source.copy();
        boolean moved = slotId < cellCount
                ? moveItemStackTo(source, playerSlotsStart, slots.size(), true)
                : moveItemStackTo(source, 0, cellCount, false);
        if (!moved) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
