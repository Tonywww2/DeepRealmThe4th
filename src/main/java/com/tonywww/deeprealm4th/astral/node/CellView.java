package com.tonywww.deeprealm4th.astral.node;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Physical cell state and optional resolved identity for one calculation round. */
public final class CellView {
    private final int index, x, y;
    private final boolean inBounds, open, effective;
    private final String inactiveReason, actualItemId, kindKey, variantKey, ruleKey, role;
    private final ItemStack item;
    private final CompoundTag instanceData;

    public CellView(int index, int x, int y, boolean inBounds, boolean open, boolean effective,
            String inactiveReason, String actualItemId, String kindKey, String variantKey,
            String ruleKey, String role, ItemStack item, CompoundTag instanceData) {
        this.index = index;
        this.x = x;
        this.y = y;
        this.inBounds = inBounds;
        this.open = open;
        this.effective = effective;
        this.inactiveReason = inactiveReason == null ? "" : inactiveReason;
        this.actualItemId = actualItemId == null ? "" : actualItemId;
        this.kindKey = kindKey == null ? "" : kindKey;
        this.variantKey = variantKey == null ? "" : variantKey;
        this.ruleKey = ruleKey == null ? "" : ruleKey;
        this.role = role == null ? "" : role;
        this.item = item == null ? ItemStack.EMPTY : item.copy();
        this.instanceData = instanceData == null ? new CompoundTag() : instanceData.copy();
    }

    public static CellView outOfBounds(int x, int y) {
        return new CellView(-1, x, y, false, false, false, "out_of_bounds", "", "", "", "", "",
                ItemStack.EMPTY, null);
    }

    public int index() { return index; }
    public int x() { return x; }
    public int y() { return y; }
    public boolean inBounds() { return inBounds; }
    public boolean open() { return open; }
    public boolean occupied() { return !item.isEmpty(); }
    public boolean effective() { return effective; }
    public String inactiveReason() { return inactiveReason; }
    public String actualItemId() { return actualItemId; }
    public String kindKey() { return kindKey; }
    public String variantKey() { return variantKey; }
    public String ruleKey() { return ruleKey; }
    public String role() { return role; }
    public ItemStack itemCopy() { return item.copy(); }
    public CompoundTag instanceData() { return instanceData.copy(); }
}
