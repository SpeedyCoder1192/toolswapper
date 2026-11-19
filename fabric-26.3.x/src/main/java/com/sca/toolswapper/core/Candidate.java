package com.sca.toolswapper.core;

import net.minecraft.world.item.ItemStack;

/**
 * A scored inventory slot.
 *
 * @param inventorySlot 0-8 hotbar, 9-35 main inventory, 40 offhand
 * @param score         higher is better; {@link Double#NEGATIVE_INFINITY} means "disqualified"
 * @param reason        short human-readable explanation, shown in the HUD and debug log
 */
public record Candidate(int inventorySlot, ItemStack stack, double score, String reason) {

    public static final Candidate NONE =
            new Candidate(-1, ItemStack.EMPTY, Double.NEGATIVE_INFINITY, "none");

    public boolean valid() {
        return inventorySlot >= 0 && score > Double.NEGATIVE_INFINITY;
    }

    public boolean isHotbar() {
        return inventorySlot >= 0 && inventorySlot < 9;
    }
}
