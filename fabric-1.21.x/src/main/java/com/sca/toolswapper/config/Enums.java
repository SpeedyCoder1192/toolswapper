package com.sca.toolswapper.config;

/** All enum options used by the config, kept in one place so GSON and the command system agree. */
public final class Enums {
    private Enums() {}

    /** When (if ever) ToolSwap puts your original item back in your hand. */
    public enum RevertMode {
        /** Never revert. The swapped tool stays selected. */
        NEVER,
        /** Revert as soon as the triggering action finishes (block broken / entity hit). */
        AFTER_ACTION,
        /** Revert N ticks after the last swap, if idle. */
        AFTER_DELAY,
        /** Revert when the crosshair stops pointing at the thing that caused the swap. */
        ON_TARGET_LOST
    }

    /** How a special-enchantment preference (silk touch, fortune) is applied. */
    public enum PreferenceMode {
        /** Built-in heuristics (silk for silk-only drops, fortune for ores/crops). */
        AUTO,
        /** Always prefer it when a tool with it exists. */
        ALWAYS,
        /** Actively avoid it. */
        NEVER,
        /** Prefer it only for blocks matching the associated list. */
        LIST_ONLY,
        /** Prefer it for everything EXCEPT blocks matching the associated list. */
        LIST_EXCLUDED
    }

    /** How weapons are ranked. */
    public enum CombatScore {
        /** Maximise damage of one fully charged hit. */
        BURST,
        /** Maximise damage per second (damage x attack speed). */
        DPS,
        /** Blend of the two. */
        HYBRID
    }

    /** Durability protection strategy. */
    public enum DurabilityGuard { OFF, ABSOLUTE, PERCENT }

    /** What causes a swap to be evaluated. */
    public enum TriggerMode {
        /** Only when you actually left-click / attack. Lowest packet volume. */
        ON_ACTION,
        /** Pre-emptively while the crosshair rests on a target. */
        ON_LOOK,
        /** Both. */
        BOTH
    }

    /** Where candidate tools may come from. */
    public enum SlotSource {
        /** Hotbar only (pure slot selection, zero container packets). */
        HOTBAR_ONLY,
        /** Hotbar first; fall back to main inventory via a SWAP click. */
        HOTBAR_THEN_INVENTORY,
        /** Consider the entire inventory equally and pull the best one up. */
        INVENTORY_PRIORITY
    }

    /** Non-HUD feedback channel. */
    public enum FeedbackStyle { NONE, ACTION_BAR, CHAT }

    /** Corner the HUD anchors to. */
    public enum HudAnchor { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
}
