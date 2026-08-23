package com.sca.toolswapper.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * Client-side weapon evaluation.
 *
 * <p>Attack damage and attack speed come from the stack's {@code ATTRIBUTE_MODIFIERS}
 * component, which is authoritative and works for modded weapons too. Enchantment damage
 * is approximated: the real calculation lives server-side in
 * {@code EnchantmentHelper#getDamage} and depends on a live {@code DamageSource}, so we
 * reproduce vanilla's Sharpness / Smite / Bane formulas directly instead.
 */
public final class CombatMath {

    /** Data-driven target tags introduced in 1.21 for the damage-boost enchantments. */
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SENSITIVE_TO_SMITE =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("minecraft", "sensitive_to_smite"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SENSITIVE_TO_BANE =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("minecraft", "sensitive_to_bane_of_arthropods"));
    private static final TagKey<net.minecraft.world.entity.EntityType<?>> SENSITIVE_TO_IMPALING =
            TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("minecraft", "sensitive_to_impaling"));

    private CombatMath() {}

    /** Total main-hand attack damage including the player's 1.0 base. */
    public static double attackDamage(ItemStack stack) {
        return attributeValue(stack, Attributes.ATTACK_DAMAGE, 1.0);
    }

    /** Attacks per second (player base is 4.0). */
    public static double attackSpeed(ItemStack stack) {
        return Math.max(0.1, attributeValue(stack, Attributes.ATTACK_SPEED, 4.0));
    }

    /** Extra damage this stack's enchantments would add against {@code target}. */
    public static double enchantDamage(ItemStack stack, Entity target) {
        double bonus = 0.0;

        int sharpness = EnchantUtil.level(stack, Enchantments.SHARPNESS);
        if (sharpness > 0) bonus += 1.0 + 0.5 * (sharpness - 1);

        if (target != null) {
            int smite = EnchantUtil.level(stack, Enchantments.SMITE);
            if (smite > 0 && target.is(SENSITIVE_TO_SMITE)) bonus += 2.5 * smite;

            int bane = EnchantUtil.level(stack, Enchantments.BANE_OF_ARTHROPODS);
            if (bane > 0 && target.is(SENSITIVE_TO_BANE)) bonus += 2.5 * bane;

            int impaling = EnchantUtil.level(stack, Enchantments.IMPALING);
            if (impaling > 0 && (target.isInWater()
                    || target.is(SENSITIVE_TO_IMPALING))) {
                bonus += 2.5 * impaling;
            }
        }
        return bonus;
    }

    private static double attributeValue(
            ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double base) {

        if (stack == null || stack.isEmpty()) return base;

        ItemAttributeModifiers comp =
                stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        double add = 0.0;
        double multBase = 0.0;
        double multTotal = 1.0;

        for (ItemAttributeModifiers.Entry entry : comp.modifiers()) {
            if (!entry.slot().test(EquipmentSlot.MAINHAND)) continue;
            if (!entry.attribute().equals(attribute)) continue;

            AttributeModifier mod = entry.modifier();
            switch (mod.operation()) {
                case ADD_VALUE -> add += mod.amount();
                case ADD_MULTIPLIED_BASE -> multBase += mod.amount();
                case ADD_MULTIPLIED_TOTAL -> multTotal *= (1.0 + mod.amount());
            }
        }

        double subtotal = base + add;
        double value = subtotal + subtotal * multBase;
        value *= multTotal;
        return value;
    }
}
