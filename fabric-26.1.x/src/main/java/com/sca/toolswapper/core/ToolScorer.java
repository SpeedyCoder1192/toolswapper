package com.sca.toolswapper.core;

import com.sca.toolswapper.config.Enums.DurabilityGuard;
import com.sca.toolswapper.config.Enums.PreferenceMode;
import com.sca.toolswapper.config.Profile;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

/** Turns "player + candidate stack + target" into a single comparable number. */
public final class ToolScorer {

    private ToolScorer() {}

    public static Candidate scoreForBlock(Player player, ItemStack stack, int slot,
                                          BlockState state, BlockPos pos, Profile profile) {

        if (!itemAllowed(stack, profile)) return disqualified(slot, stack, "item filtered");

        Profile.Mining m = profile.mining;
        double ticks = MiningMath.breakTicks(player, stack, state, pos);
        if (!Double.isFinite(ticks)) return disqualified(slot, stack, "unbreakable");

        boolean correctTool = MiningMath.canHarvest(player, stack, state);
        if (m.requireCorrectTool && !correctTool) {
            return disqualified(slot, stack, "wrong tool");
        }

        StringBuilder why = new StringBuilder();
        double score = -ticks * m.tickCost;

        if (correctTool) {
            score += m.correctToolBonus;
        }
        if (m.preferInstantMine && ticks <= 1.0) {
            score += m.instantMineBonus;
            why.append("instant ");
        }

        if (m.useShearsForShearable && stack.getItem() instanceof ShearsItem
                && Matchers.matchesBlock(state, m.shearBlocks)) {
            score += m.specialToolBonus + m.shearsPreferenceBonus;
            why.append("shears ");
        }
        if (m.useSwordForInstantBlocks && stack.is(ItemTags.SWORDS)
                && Matchers.matchesBlock(state, m.swordBlocks)) {
            score += m.specialToolBonus;
            why.append("sword ");
        }

        for (Map.Entry<String, String> e : m.blockOverrides.entrySet()) {
            if (!Matchers.matchesBlock(state, e.getKey())) continue;
            if (Matchers.matchesItem(stack, e.getValue())) {
                score += m.blockOverrideBonus;
                why.append("override ");
            }
            break;
        }

        boolean silk = EnchantUtil.has(stack, Enchantments.SILK_TOUCH);
        int fortune = EnchantUtil.level(stack, Enchantments.FORTUNE);

        boolean wantSilk = wants(m.silkTouch, state, m.silkTouchBlocks, silk);
        boolean wantFortune = wants(m.fortune, state, m.fortuneBlocks, fortune > 0);

        if (silk) {
            if (wantSilk) { score += m.silkTouchBonus; why.append("silk "); }
            else if (m.silkTouch == PreferenceMode.NEVER) { score -= m.silkTouchBonus; }
            else if (wantFortune) { score -= m.silkTouchBonus; }
        }
        if (fortune > 0 && wantFortune && !silk) {
            score += m.fortuneBonusPerLevel * fortune;
            why.append("fortune").append(fortune).append(' ');
        }

        if (m.preferLowestSufficientTier) {
            score -= tierIndex(stack) * m.tierPenalty;
        }
        if (m.durabilityWeight != 0.0) {
            score += m.durabilityWeight * durabilityFraction(stack);
        }

        score += priorityBonus(stack, profile);

        Double guard = durabilityGuardPenalty(stack, profile);
        if (guard == null) return disqualified(slot, stack, "durability guard");
        score += guard;

        String reason = why.isEmpty()
                ? String.format("%.0ft", ticks)
                : String.format("%.0ft %s", ticks, why.toString().trim());
        return new Candidate(slot, stack, score, reason);
    }

    public static Candidate scoreForEntity(Player player, ItemStack stack, int slot,
                                           Entity target, Profile profile) {

        if (!itemAllowed(stack, profile)) return disqualified(slot, stack, "item filtered");

        Profile.Combat c = profile.combat;
        double damage = CombatMath.attackDamage(stack) + CombatMath.enchantDamage(stack, target);
        double speed = CombatMath.attackSpeed(stack);

        double score = switch (c.scoring) {
            case BURST -> damage * 100.0;
            case DPS -> damage * speed * 100.0;
            case HYBRID -> damage * Math.sqrt(speed) * 100.0;
        };

        StringBuilder why = new StringBuilder(String.format("%.1f dmg ", damage));

        if (c.preferAxeVsShield && stack.getItem() instanceof AxeItem && isShielded(target)) {
            score += 400.0;
            why.append("anti-shield ");
        }
        if (c.preferTridentInWater && stack.getItem() instanceof TridentItem
                && (player.isInWater() || target.isInWater())) {
            score += 300.0;
            why.append("trident ");
        }

        int knockback = EnchantUtil.level(stack, Enchantments.KNOCKBACK);
        if (c.avoidKnockback && knockback > 0) {
            score -= c.knockbackPenaltyPerLevel * knockback;
            why.append("-kb ");
        }
        if (c.lootingBonusPerLevel != 0.0) {
            score += c.lootingBonusPerLevel * EnchantUtil.level(stack, Enchantments.LOOTING);
        }
        if (c.fireAspectBonus != 0.0 && EnchantUtil.has(stack, Enchantments.FIRE_ASPECT)) {
            score += c.fireAspectBonus;
        }
        if (c.sweepingEdgeBonus != 0.0) {
            score += c.sweepingEdgeBonus * EnchantUtil.level(stack, Enchantments.SWEEPING_EDGE);
        }

        for (Map.Entry<String, String> e : c.entityOverrides.entrySet()) {
            if (Matchers.matchesEntity(target, e.getKey())
                    && Matchers.matchesItem(stack, e.getValue())) {
                score += c.overrideBonus;
                why.append("override ");
                break;
            }
        }

        score += priorityBonus(stack, profile);

        Double guard = durabilityGuardPenalty(stack, profile);
        if (guard == null) return disqualified(slot, stack, "durability guard");
        score += guard;

        return new Candidate(slot, stack, score, why.toString().trim());
    }

    private static Candidate disqualified(int slot, ItemStack stack, String reason) {
        return new Candidate(slot, stack, Double.NEGATIVE_INFINITY, reason);
    }

    private static boolean itemAllowed(ItemStack stack, Profile profile) {
        if (stack == null || stack.isEmpty()) return false;
        Profile.Items items = profile.items;
        if (Matchers.matchesItem(stack, items.blacklist)) return false;
        if (items.whitelistOnly && !Matchers.matchesItem(stack, items.whitelist)) return false;
        return true;
    }

    private static double priorityBonus(ItemStack stack, Profile profile) {
        double total = 0.0;
        for (Map.Entry<String, Double> e : profile.items.priorityBonus.entrySet()) {
            if (Matchers.matchesItem(stack, e.getKey())) total += e.getValue();
        }
        return total;
    }

    /**
     * @return additional score, or {@code null} if the stack is disqualified by the guard
     */
    private static Double durabilityGuardPenalty(ItemStack stack, Profile profile) {
        Profile.Safety s = profile.safety;
        if (!stack.isDamageableItem()) return 0.0;

        boolean protectedItem = Matchers.matchesItem(stack, profile.items.neverRisk);
        if (s.durabilityGuard == DurabilityGuard.OFF && !protectedItem) return 0.0;
        if (s.guardOnlyValuable && !protectedItem
                && !stack.isEnchanted() && !stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
            return 0.0;
        }

        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        boolean tooLow = switch (s.durabilityGuard) {
            case OFF -> false;
            case ABSOLUTE -> remaining <= s.minDurabilityAbsolute;
            case PERCENT -> (double) remaining / stack.getMaxDamage() <= s.minDurabilityPercent;
        };
        if (protectedItem) tooLow = tooLow || remaining <= Math.max(1, s.minDurabilityAbsolute);

        if (!tooLow) return 0.0;
        return s.allowGuardedIfNoAlternative ? -1_000_000.0 : null;
    }

    private static double durabilityFraction(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() == 0) return 1.0;
        return 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage();
    }

    /**
     * Rough material ranking used only by the "save the good gear" preference.
     * Derived from max durability rather than {@code ToolMaterial}, because that class
     * was restructured during 1.21.x - and this also ranks modded tools sensibly.
     */
    private static int tierIndex(ItemStack stack) {
        if (!stack.isDamageableItem()) return 0;
        int max = stack.getMaxDamage();
        if (max <= 80) return 0;
        if (max <= 160) return 1;
        if (max <= 400) return 2;
        if (max <= 1700) return 3;
        return 4;
    }

    private static boolean isShielded(Entity target) {
        if (!(target instanceof LivingEntity living)) return false;
        return living.getMainHandItem().is(Items.SHIELD)
                || living.getOffhandItem().is(Items.SHIELD);
    }

    /** Resolves a {@link PreferenceMode} against the block and its list. */
    private static boolean wants(PreferenceMode mode, BlockState state,
                                 java.util.List<String> list, boolean stackHasIt) {
        if (!stackHasIt) return false;
        return switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case LIST_ONLY, AUTO -> Matchers.matchesBlock(state, list);
            case LIST_EXCLUDED -> !Matchers.matchesBlock(state, list);
        };
    }
}
