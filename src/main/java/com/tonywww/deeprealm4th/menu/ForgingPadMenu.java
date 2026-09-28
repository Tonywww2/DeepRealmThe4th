package com.tonywww.deeprealm4th.menu;

import com.tonywww.deeprealm4th.blockentity.ForgingPadBlockEntity;
import com.tonywww.deeprealm4th.platform.recipe.AstralProcessLookup;
import com.tonywww.deeprealm4th.platform.recipe.CombinationForgingRecipe;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralMenuRegistration;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Twelve persistent inputs, with click progress owned by this open menu only. */
public final class ForgingPadMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = ForgingPadBlockEntity.SLOTS;
    private final Container pad;
    private final BlockPos pos;
    private final Player player;
    private final SimpleContainerData progress = new SimpleContainerData(1);
    private long lastAcceptedTick = Long.MIN_VALUE;
    private long inputSignature = Long.MIN_VALUE;

    public ForgingPadMenu(int id, Inventory inventory, BlockPos pos, Container pad) {
        super(AstralMenuRegistration.FORGING_PAD.get(), id);
        this.pad = pad;
        this.pos = pos;
        this.player = inventory.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new Slot(pad, row * 4 + col, 15 + col * 18, 29 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + (row + 1) * 9, 39 + col * 18, 121 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 39 + col * 18, 179));
        }
        addDataSlots(progress);
    }

    public static ForgingPadMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        return new ForgingPadMenu(id, inventory, buffer.readBlockPos(),
                new SimpleContainer(ForgingPadBlockEntity.SLOTS));
    }

    public BlockPos blockPos() { return pos; }
    public int clicksDone() { return progress.get(0); }

    public CombinationForgingRecipe matchingRecipe() {
        return AstralProcessLookup.forging(player.level()).stream()
                .sorted(Comparator.comparing(AstralProcessLookup.ForgingEntry::id))
                .map(AstralProcessLookup.ForgingEntry::recipe)
                .filter(recipe -> recipe.assignment(pad) != null)
                .findFirst().orElse(null);
    }

    @Override public void broadcastChanges() {
        if (!player.level().isClientSide) refreshInputs();
        super.broadcastChanges();
    }

    private void refreshInputs() {
        long signature = 1125899906842597L;
        for (int i = 0; i < ForgingPadBlockEntity.SLOTS; i++) {
            ItemStack stack = pad.getItem(i);
            signature = signature * 31 + BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
            signature = signature * 31 + stack.getCount();
        }
        if (signature != inputSignature) {
            inputSignature = signature;
            progress.set(0, 0);
        }
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0) return false;
        if (player.level().isClientSide) return true;
        if (!stillValid(player)) return false;
        refreshInputs();
        CombinationForgingRecipe recipe = matchingRecipe();
        if (recipe == null) return false;
        long now = player.level().getGameTime();
        boolean cooling = lastAcceptedTick != Long.MIN_VALUE && now - lastAcceptedTick < recipe.cooldown();
        if (cooling) {
            sound(0.2F, 1.5F);
            return true;
        }
        boolean completing = progress.get(0) + 1 >= recipe.clicks();
        if (completing && !player.getAbilities().instabuild && player.experienceLevel < recipe.levels()) {
            sound(0.2F, 0.8F);
            return true;
        }
        sound(0.65F, 1.0F);
        lastAcceptedTick = now;
        progress.set(0, progress.get(0) + 1);
        if (completing) {
            int[] assignment = recipe.assignment(pad);
            if (assignment == null) { progress.set(0, 0); return true; }
            for (int i = 0; i < assignment.length; i++) {
                pad.removeItem(assignment[i], recipe.inputs().get(i).count());
            }
            if (!player.getAbilities().instabuild) player.giveExperienceLevels(-recipe.levels());
            ItemStack output = recipe.output();
            if (!output.isEmpty()) {
                player.getInventory().add(output);
                if (!output.isEmpty()) Block.popResource(player.level(), pos, output);
            }
            progress.set(0, 0);
            inputSignature = Long.MIN_VALUE;
        }
        broadcastChanges();
        return true;
    }

    private void sound(float volume, float pitch) {
        player.level().playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, volume, pitch);
    }

    @Override public boolean stillValid(Player player) {
        return player.level().isClientSide || player.level().getBlockState(pos).is(AstralBlockRegistration.FORGING_PAD.get())
                && player.level().getBlockEntity(pos) == pad && pad.stillValid(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, PLAYER_START, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
