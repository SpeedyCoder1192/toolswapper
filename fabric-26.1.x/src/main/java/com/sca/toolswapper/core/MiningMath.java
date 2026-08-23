package com.sca.toolswapper.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Break-time prediction for a hypothetical held item.
 *
 * <p>Vanilla's break speed is
 * {@code (toolSpeed + miningEfficiency) * hasteFactor * fatigueFactor * waterFactor * airFactor}.
 * Only the first term depends on which item you hold, so instead of re-deriving every
 * situational multiplier (their mapping names move around between 1.21 point releases) we
 * measure them once by dividing the game's own answer for the held item by our computed
 * tool term. Everything else falls out of that ratio.
 */
public final class MiningMath {

    private MiningMath() {}

    /** Raw item contribution: base tool speed plus the Efficiency attribute bonus. */
    public static float toolSpeed(ItemStack stack, BlockState state) {
        if (stack == null) return 1.0f;
        float base = stack.getDestroySpeed(state);
        if (base > 1.0f) {
            int eff = EnchantUtil.level(stack, Enchantments.EFFICIENCY);
            if (eff > 0) base += (float) (eff * eff) + 1.0f;
        }
        return base;
    }

    /** Everything about the player that is not the held item (haste, fatigue, water, airborne). */
    public static float situationalMultiplier(Player player, BlockState state) {
        float heldTerm = toolSpeed(player.getMainHandItem(), state);
        if (heldTerm <= 0.0f) return 1.0f;
        float actual = player.getDestroySpeed(state);
        float ratio = actual / heldTerm;
        return (Float.isFinite(ratio) && ratio > 0.0f) ? ratio : 1.0f;
    }

    public static boolean canHarvest(Player player, ItemStack stack, BlockState state) {
        return !state.requiresCorrectToolForDrops() || (stack != null && stack.isCorrectToolForDrops(state));
    }

    /**
     * Predicted ticks to break {@code state} with {@code stack}.
     *
     * @return 0 for instant, {@link Double#POSITIVE_INFINITY} for unbreakable
     */
    public static double breakTicks(Player player, ItemStack stack, BlockState state, BlockPos pos) {
        float hardness = state.getDestroySpeed(player.level(), pos);
        if (hardness < 0.0f) return Double.POSITIVE_INFINITY;
        if (hardness == 0.0f) return 0.0;

        float speed = toolSpeed(stack, state) * situationalMultiplier(player, state);
        int divisor = canHarvest(player, stack, state) ? 30 : 100;
        double perTick = speed / hardness / divisor;

        if (!(perTick > 0.0)) return Double.POSITIVE_INFINITY;
        if (perTick >= 1.0) return 0.0;
        return Math.ceil(1.0 / perTick);
    }
}
