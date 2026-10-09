package com.tonywww.deeprealm4th.menu;

import com.tonywww.deeprealm4th.blockentity.ForgingPadBlockEntity;
import com.tonywww.deeprealm4th.platform.recipe.AstralProcessLookup;
import com.tonywww.deeprealm4th.platform.recipe.CombinationForgingRecipe;
import com.tonywww.deeprealm4th.astral.process.AstralTransforms;
import com.tonywww.deeprealm4th.platform.registry.AstralBlockRegistration;
import com.tonywww.deeprealm4th.platform.registry.AstralMenuRegistration;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
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
    private ItemStack[] inputSnapshot;

    public ForgingPadMenu(int id, Inventory inventory, BlockPos pos, Container pad) {
        super(AstralMenuRegistration.FORGING_PAD.get(), id);
        this.pad = pad;
        this.pos = pos;
        this.player = inventory.player;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new Slot(pad, row * 4 + col, 47 + col * 18, 29 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + (row + 1) * 9, 64 + col * 18, 121 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 64 + col * 18, 179));
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
        AstralProcessLookup.ForgingEntry entry = matchingEntry();
        return entry == null ? null : entry.recipe();
    }

    public AstralProcessLookup.ForgingEntry matchingEntry() {
        return AstralProcessLookup.forging(player.level()).stream()
                .sorted(Comparator.comparing(AstralProcessLookup.ForgingEntry::id))
                .filter(entry -> entry.recipe().assignment(pad) != null)
                .findFirst().orElse(null);
    }

    public ItemStack previewOutput() {
        AstralProcessLookup.ForgingEntry entry = matchingEntry();
        if (entry == null) return ItemStack.EMPTY;
        AstralTransforms.Result<ItemStack> preview = entry.recipe().preview(player, pad, entry.id());
        return preview.ok() ? preview.value() : ItemStack.EMPTY;
    }

    @Override public void broadcastChanges() {
        if (!player.level().isClientSide) refreshInputs();
        super.broadcastChanges();
    }

    private void refreshInputs() {
        boolean changed = inputSnapshot == null;
        ItemStack[] current = new ItemStack[ForgingPadBlockEntity.SLOTS];
        for (int i = 0; i < ForgingPadBlockEntity.SLOTS; i++) {
            current[i] = pad.getItem(i).copy();
            if (!changed && !ItemStack.matches(inputSnapshot[i], current[i])) changed = true;
        }
        if (changed) progress.set(0, 0);
        inputSnapshot = current;
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (button != 0) return false;
        if (player.level().isClientSide) return true;
        if (!stillValid(player)) return false;
        refreshInputs();
        AstralProcessLookup.ForgingEntry entry = matchingEntry();
        if (entry == null) return false;
        CombinationForgingRecipe recipe = entry.recipe();
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
        int[] assignment = null;
        int[] consume = null;
        AstralTransforms.ProcessOutput produced = null;
        if (completing) {
            assignment = recipe.assignment(pad);
            if (assignment == null) { progress.set(0, 0); return true; }
            ItemStack[] before = new ItemStack[pad.getContainerSize()];
            for (int i = 0; i < before.length; i++) before[i] = pad.getItem(i).copy();
            AstralTransforms.Result<AstralTransforms.ProcessOutput> outcome = recipe.produce(player, pad, entry.id());
            if (!outcome.ok()) {
                progress.set(0, 0);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(outcome.error()), true);
                return true;
            }
            produced = outcome.value();
            consume = new int[assignment.length];
            java.util.Set<String> known = new java.util.HashSet<>();
            for (int i = 0; i < assignment.length; i++) {
                String name = recipe.inputs().get(i).name().isEmpty() ? "input_" + i : recipe.inputs().get(i).name();
                known.add(name);
                consume[i] = produced.consumed().getOrDefault(name, recipe.inputs().get(i).count());
                if (consume[i] < 0 || consume[i] > before[assignment[i]].getCount()) return true;
            }
            if (!known.containsAll(produced.consumed().keySet())) return true;
            for (int i = 0; i < before.length; i++)
                if (!ItemStack.matches(before[i], pad.getItem(i))) return true;
            if (recipe.assignment(pad) == null) return true;
        }
        sound(0.65F, 1.0F);
        lastAcceptedTick = now;
        progress.set(0, progress.get(0) + 1);
        if (completing) {
            for (int i = 0; i < assignment.length; i++) {
                pad.removeItem(assignment[i], consume[i]);
            }
            if (!player.getAbilities().instabuild) player.giveExperienceLevels(-recipe.levels());
            ItemStack output = produced.result();
            if (!output.isEmpty()) {
                player.getInventory().add(output);
                if (!output.isEmpty()) Block.popResource(player.level(), pos, output);
            }
            for (ItemStack returned : produced.returned()) {
                if (returned.isEmpty()) continue;
                player.getInventory().add(returned);
                if (!returned.isEmpty()) Block.popResource(player.level(), pos, returned);
            }
            progress.set(0, 0);
            inputSnapshot = null;
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
