package com.tonywww.deeprealm4th.astral.node;

import com.tonywww.deeprealm4th.platform.attributes.AstralAttributeValues;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Common player/world condition view shared by every node in one round. */
public final class PlayerStateSnapshot {
    private final Player player;
    private final double health, maxHealth, experienceProgress, mainHandRemaining;
    private final int experienceLevel, food, mainHandDamage;
    private final boolean onGround, crouching, raining, thundering, mainHandDamageable;
    private final long gameTime, dayTime;
    private final String dimension, mainHandItem;
    private final Map<String, Double> attributes = new HashMap<>();

    private PlayerStateSnapshot(Player player) {
        this.player = player;
        health = player.getHealth();
        maxHealth = player.getMaxHealth();
        experienceLevel = player.experienceLevel;
        experienceProgress = player.experienceProgress;
        food = player.getFoodData().getFoodLevel();
        onGround = player.onGround();
        crouching = player.isCrouching();
        raining = player.level().isRaining();
        thundering = player.level().isThundering();
        gameTime = player.level().getGameTime();
        dayTime = player.level().getDayTime();
        dimension = player.level().dimension().location().toString();
        ItemStack hand = player.getMainHandItem();
        mainHandItem = BuiltInRegistries.ITEM.getKey(hand.getItem()).toString();
        mainHandDamageable = hand.isDamageableItem();
        mainHandDamage = hand.getDamageValue();
        mainHandRemaining = mainHandDamageable && hand.getMaxDamage() > 0
                ? (double) (hand.getMaxDamage() - mainHandDamage) / hand.getMaxDamage() : 1;
    }

    public static PlayerStateSnapshot capture(Player player) { return new PlayerStateSnapshot(player); }
    public double health() { return health; }
    public double maxHealth() { return maxHealth; }
    public double healthRatio() { return maxHealth > 0 ? health / maxHealth : 0; }
    public int experienceLevel() { return experienceLevel; }
    public double experienceProgress() { return experienceProgress; }
    public int food() { return food; }
    public boolean onGround() { return onGround; }
    public boolean crouching() { return crouching; }
    public boolean raining() { return raining; }
    public boolean thundering() { return thundering; }
    public long gameTime() { return gameTime; }
    public long dayTime() { return dayTime; }
    public String dimension() { return dimension; }
    public String mainHandItem() { return mainHandItem; }
    public boolean mainHandDamageable() { return mainHandDamageable; }
    public int mainHandDamage() { return mainHandDamage; }
    public double mainHandRemaining() { return mainHandRemaining; }
    /** NaN signals an unregistered or absent attribute. Values are memoized for the round. */
    public double attribute(String id) { return attributes.computeIfAbsent(id, key -> AstralAttributeValues.get(player, key)); }
    public boolean hasUnlockTag(String tag) { return player.getTags().contains(tag); }
}
