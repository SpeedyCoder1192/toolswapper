package com.sca.toolswapper.core;

import com.sca.toolswapper.config.Enums.SlotSource;
import com.sca.toolswapper.config.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Walks the inventory, scores every legal slot and returns the winner. */
public final class SlotFinder {

    public static final int OFFHAND_SLOT = 40;

    private SlotFinder() {}

    public static Result bestForBlock(LocalPlayer player, BlockState state,
                                      BlockPos pos, Profile profile) {
        return best(player, profile,
                (stack, slot) -> ToolScorer.scoreForBlock(player, stack, slot, state, pos, profile));
    }

    public static Result bestForEntity(LocalPlayer player, Entity target, Profile profile) {
        return best(player, profile,
                (stack, slot) -> ToolScorer.scoreForEntity(player, stack, slot, target, profile));
    }

    private static Result best(LocalPlayer player, Profile profile,
                               BiFunction<ItemStack, Integer, Candidate> scorer) {

        Inventory inv = player.getInventory();
        Profile.General g = profile.general;

        List<Candidate> all = new ArrayList<>();
        Candidate best = Candidate.NONE;
        Candidate current = Candidate.NONE;

        int selected = inv.getSelectedSlot();

        for (int slot = 0; slot < 9; slot++) {
            if (!g.allowedHotbarSlots.contains(slot) && slot != selected) continue;
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty()) continue;
            Candidate c = scorer.apply(stack, slot);
            all.add(c);
            if (slot == selected) current = c;
            if (c.score() > best.score()) best = c;
        }

        if (g.slotSource != SlotSource.HOTBAR_ONLY) {
            boolean hotbarAlreadyGood =
                    g.slotSource == SlotSource.HOTBAR_THEN_INVENTORY && best.valid()
                            && best.score() >= current.score() + Math.max(0.0, g.requiredScoreGain);

            if (!hotbarAlreadyGood || g.slotSource == SlotSource.INVENTORY_PRIORITY) {
                for (int slot = 9; slot < 36; slot++) {
                    ItemStack stack = inv.getItem(slot);
                    if (stack.isEmpty()) continue;
                    Candidate c = scorer.apply(stack, slot);
                    all.add(c);
                    if (c.score() > best.score()) best = c;
                }
            }
        }

        if (!g.protectOffhand) {
            ItemStack off = player.getOffhandItem();
            if (!off.isEmpty()) {
                Candidate c = scorer.apply(off, OFFHAND_SLOT);
                all.add(c);
                if (c.score() > best.score()) best = c;
            }
        }

        return new Result(best, current, all);
    }

    /**
     * Finds the best slot holding an item matching {@code itemPattern}. Used by the
     * utility (right-click) module, where "correct item" beats "highest score".
     */
    public static Candidate firstMatching(LocalPlayer player, Profile profile, String itemPattern) {
        Inventory inv = player.getInventory();
        Profile.General g = profile.general;
        Candidate best = Candidate.NONE;

        int limit = g.slotSource == SlotSource.HOTBAR_ONLY ? 9 : 36;
        for (int slot = 0; slot < limit; slot++) {
            if (slot < 9 && !g.allowedHotbarSlots.contains(slot)) continue;
            ItemStack stack = inv.getItem(slot);
            if (stack.isEmpty() || !Matchers.matchesItem(stack, itemPattern)) continue;

            double score = (slot < 9 ? 1000.0 : 0.0)
                    + (stack.isDamageableItem()
                        ? 100.0 * (1.0 - (double) stack.getDamageValue() / stack.getMaxDamage())
                        : 100.0);
            if (score > best.score()) {
                best = new Candidate(slot, stack, score, "utility match");
            }
        }
        return best;
    }

    /**
     * @param best    highest scoring legal slot
     * @param current the score of what is already held (for gain thresholds)
     * @param all     every scored candidate, used by the debug log
     */
    public record Result(Candidate best, Candidate current, List<Candidate> all) {
        public boolean worthSwapping(Profile profile) {
            Profile.General g = profile.general;
            if (!best.valid()) return false;
            if (best.inventorySlot() == current.inventorySlot()) return false;
            if (g.skipIfCurrentIsBest && current.valid()
                    && current.score() >= best.score()) return false;
            double gain = best.score() - (current.valid() ? current.score() : Double.NEGATIVE_INFINITY);
            return gain >= g.requiredScoreGain;
        }
    }
}
