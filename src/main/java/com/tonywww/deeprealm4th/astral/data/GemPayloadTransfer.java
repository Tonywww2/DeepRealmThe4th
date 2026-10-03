package com.tonywww.deeprealm4th.astral.data;

import com.tonywww.deeprealm4th.astral.tooltip.FillerPresentations;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** Deterministic transfer helpers: processing never rerolls a gem's stored traits. */
public final class GemPayloadTransfer {
    private GemPayloadTransfer() {}

    public record TransferResult(ItemStack stack, String error) {
        public boolean ok() { return error == null; }
    }

    public static TransferResult toFiller(ItemStack source, String sourceKey,
            ItemStack fillerTemplate, String fillerKey) {
        GemPayload.DecodeResult decoded = read(source, sourceKey);
        if (!decoded.ok()) return new TransferResult(null, decoded.error());
        String actual = BuiltInRegistries.ITEM.getKey(source.getItem()).toString();
        if (!actual.equals(decoded.value().sourceItem()))
            return new TransferResult(null, "source_item_mismatch");
        if (fillerTemplate.isEmpty()) return new TransferResult(null, "missing_filler_template");
        ItemStack filler = fillerTemplate.copyWithCount(1);
        return new TransferResult(FillerPresentations.applyNameCopy(AstralItemData.writeCopy(
                filler, fillerKey, decoded.value().encode())), null);
    }

    public static TransferResult embed(ItemStack gem, String gemKey, ItemStack badge,
            String badgeKey) {
        GemPayload.DecodeResult decoded = read(gem, gemKey);
        if (!decoded.ok()) return new TransferResult(null, decoded.error());
        if (badge.isEmpty()) return new TransferResult(null, "missing_badge_template");
        CompoundTag badgeData = AstralItemData.read(badge, badgeKey);
        if (badgeData == null) badgeData = new CompoundTag();
        if (badgeData.contains("gem", Tag.TAG_COMPOUND))
            return new TransferResult(null, "badge_socket_occupied");
        badgeData.put("gem", decoded.value().encode());
        return new TransferResult(FillerPresentations.applyNameCopy(AstralItemData.writeCopy(
                badge.copyWithCount(1), badgeKey, badgeData)), null);
    }

    public static TransferResult update(ItemStack stack, String key,
            UnaryOperator<GemPayload> change) {
        GemPayload.DecodeResult decoded = read(stack, key);
        if (!decoded.ok()) return new TransferResult(null, decoded.error());
        try {
            GemPayload updated = Objects.requireNonNull(change.apply(decoded.value()), "updated payload");
            if (!updated.identity().equals(decoded.value().identity()))
                return new TransferResult(null, "gem_identity_changed");
            return new TransferResult(FillerPresentations.applyNameCopy(AstralItemData.writeCopy(
                    stack, key, updated.encode())), null);
        } catch (RuntimeException exception) {
            return new TransferResult(null, "gem_update_error:" + exception.getMessage());
        }
    }

    public static TransferResult updateEmbedded(ItemStack badge, String badgeKey,
            UnaryOperator<GemPayload> change) {
        CompoundTag badgeData = AstralItemData.read(badge, badgeKey);
        if (badgeData == null || !badgeData.contains("gem", Tag.TAG_COMPOUND))
            return new TransferResult(null, "badge_socket_empty");
        GemPayload.DecodeResult decoded = GemPayload.decode(badgeData.getCompound("gem"));
        if (!decoded.ok()) return new TransferResult(null, decoded.error());
        try {
            GemPayload updated = Objects.requireNonNull(change.apply(decoded.value()), "updated payload");
            if (!updated.identity().equals(decoded.value().identity()))
                return new TransferResult(null, "gem_identity_changed");
            badgeData.put("gem", updated.encode());
            return new TransferResult(FillerPresentations.applyNameCopy(AstralItemData.writeCopy(
                    badge, badgeKey, badgeData)), null);
        } catch (RuntimeException exception) {
            return new TransferResult(null, "gem_update_error:" + exception.getMessage());
        }
    }

    public static GemPayload.DecodeResult read(ItemStack stack, String key) {
        CompoundTag data = AstralItemData.read(stack, key);
        return GemPayload.decode(data);
    }
}
