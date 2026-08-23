package com.sca.toolswapper.core;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Enchantment level lookup that avoids touching the dynamic registry.
 *
 * <p>Since 1.21 enchantments are data-driven, so a stack's enchantments are
 * {@code RegistryEntry<Enchantment>} references. Comparing those entries against a
 * {@link ResourceKey} with {@code matchesKey} works across every 1.21.x point release,
 * whereas the registry-lookup helpers were renamed several times.
 */
public final class EnchantUtil {

    private EnchantUtil() {}

    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        if (stack == null || stack.isEmpty() || key == null) return 0;
        ItemEnchantments comp = EnchantmentHelper.getEnchantmentsForCrafting(stack);
        if (comp == null || comp.isEmpty()) return 0;
        for (var entry : comp.entrySet()) {
            if (entry.getKey().is(key)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static boolean has(ItemStack stack, ResourceKey<Enchantment> key) {
        return level(stack, key) > 0;
    }
}
