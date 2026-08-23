package com.sca.toolswapper.config;

import com.sca.toolswapper.config.Enums.*;
import com.sca.toolswapper.core.Matchers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One complete, independently tunable behaviour set. You can have as many of these
 * as you like and hot-swap between them with a keybind, a command, or automatically
 * per-dimension.
 *
 * <p>Every string list here accepts the pattern syntax documented in
 * {@link Matchers}: exact ids, {@code *} wildcards,
 * {@code #tag} references and {@code !} negation.
 */
public class Profile {

    public String description = "";

    public General general = new General();
    public Mining mining = new Mining();
    public Combat combat = new Combat();
    public Utility utility = new Utility();
    public Filters filters = new Filters();
    public Items items = new Items();
    public Safety safety = new Safety();

    public static class General {
        /** Master switch for this profile. */
        public boolean enabled = true;

        /** Where tools may be pulled from. */
        public SlotSource slotSource = SlotSource.HOTBAR_ONLY;

        /** Hotbar slots (0-8) ToolSwap is allowed to select. Leave the rest for your own use. */
        public List<Integer> allowedHotbarSlots = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8));

        /** Hotbar slot used as the landing pad for inventory pulls. -1 = the currently selected slot. */
        public int stagingHotbarSlot = -1;

        /** Ticks to wait after a trigger before actually swapping. 0 = instant. */
        public int swapDelayTicks = 0;

        /** Minimum ticks between two swaps. */
        public int swapCooldownTicks = 0;

        /** Hard cap on hotbar selections per second. */
        public int maxSwapsPerSecond = 20;

        /** Hard cap on inventory SWAP clicks per second (much more visible to servers). */
        public int maxInventorySwapsPerSecond = 4;

        public RevertMode revertMode = RevertMode.NEVER;
        public int revertDelayTicks = 40;

        /** Only revert if you have not manually changed slots since the swap. */
        public boolean revertOnlyIfUntouched = true;

        /** Never pull an item out of the offhand slot. */
        public boolean protectOffhand = true;

        /** If the held item is already the winner, do nothing (no packet at all). */
        public boolean skipIfCurrentIsBest = true;

        /** A candidate must beat the held item by at least this much score to trigger a swap. */
        public double requiredScoreGain = 0.0;
    }

    public static class Mining {
        public boolean enabled = true;
        public TriggerMode trigger = TriggerMode.ON_ACTION;

        /** Re-evaluate every tick while you hold left click (useful for mixed-block tunnels). */
        public boolean swapMidBreak = false;

        /** Don't bother swapping for blocks the held item already breaks in one tick. */
        public boolean skipInstantBreakBlocks = true;

        /** Candidate must be at least this many times faster than the held item. 1.0 = any improvement. */
        public double minSpeedGainRatio = 1.0;

        /** Refuse to swap to anything that isn't the "correct" tool, even if it's faster. */
        public boolean requireCorrectTool = false;

        /** Strongly favour any tool that reaches instant-mine (1 tick or less). */
        public boolean preferInstantMine = true;

        /** Among tools that tie on break time, pick the cheapest material tier. */
        public boolean preferLowestSufficientTier = false;
        /** Score penalty per tier step when preferLowestSufficientTier is on. */
        public double tierPenalty = 0.25;

        /** Score bonus per unit of remaining-durability fraction. Positive = prefer healthy tools. */
        public double durabilityWeight = 0.0;

        public PreferenceMode silkTouch = PreferenceMode.AUTO;
        public List<String> silkTouchBlocks = new ArrayList<>(List.of(
                "#minecraft:ice", "minecraft:glass", "minecraft:*_glass", "minecraft:*_glass_pane",
                "minecraft:glowstone", "minecraft:sea_lantern", "minecraft:bookshelf",
                "minecraft:spawner", "minecraft:budding_amethyst", "minecraft:mycelium",
                "minecraft:podzol", "minecraft:grass_block", "minecraft:turtle_egg"
        ));

        public PreferenceMode fortune = PreferenceMode.AUTO;
        public List<String> fortuneBlocks = new ArrayList<>(List.of(
                "#minecraft:coal_ores", "#minecraft:diamond_ores", "#minecraft:emerald_ores",
                "#minecraft:lapis_ores", "#minecraft:redstone_ores", "#minecraft:copper_ores",
                "minecraft:nether_gold_ore", "minecraft:gilded_blackstone",
                "minecraft:amethyst_cluster", "#minecraft:crops", "minecraft:nether_wart",
                "minecraft:sweet_berry_bush", "minecraft:glowstone", "minecraft:melon", "minecraft:gravel",
                "!minecraft:spawner"
        ));

        public boolean useShearsForShearable = true;
        public List<String> shearBlocks = new ArrayList<>(List.of(
                "#minecraft:leaves", "#minecraft:wool", "minecraft:cobweb", "minecraft:vine",
                "minecraft:glow_lichen", "minecraft:nether_sprouts",
                "minecraft:seagrass", "minecraft:tall_seagrass"
        ));

        /**
         * Explicit block -&gt; item rules for when you want to overrule the scorer outright.
         * First matching block pattern wins; if you hold nothing matching its item pattern,
         * scoring falls through to normal behaviour.
         * Example: {@code "#minecraft:leaves": "minecraft:shears"}
         */
        public Map<String, String> blockOverrides = new LinkedHashMap<>();
        public double blockOverrideBonus = 5000.0;

        public boolean useSwordForInstantBlocks = true;
        public List<String> swordBlocks = new ArrayList<>(List.of(
                "minecraft:cobweb", "minecraft:bamboo", "minecraft:bamboo_sapling"
        ));

        public double correctToolBonus = 1000.0;
        public double silkTouchBonus = 500.0;
        public double fortuneBonusPerLevel = 120.0;
        public double instantMineBonus = 2000.0;
        public double specialToolBonus = 1500.0;
        public double shearsPreferenceBonus = 100.0;
        /** Score cost of each tick of break time. */
        public double tickCost = 1.0;
    }

    public static class Combat {
        public boolean enabled = true;
        public TriggerMode trigger = TriggerMode.ON_ACTION;
        public CombatScore scoring = CombatScore.BURST;

        /** Don't swap if it would land mid-charge and waste your attack cooldown. */
        public boolean respectAttackCooldown = true;
        /** Cooldown progress (0-1) required before a combat swap is allowed. */
        public double minCooldownProgress = 0.9;

        /** Favour axes against entities holding a shield (players, piglins). */
        public boolean preferAxeVsShield = true;
        /** Favour a trident when you or the target are in water. */
        public boolean preferTridentInWater = false;

        /** Avoid knockback enchants entirely (mob farms, ledge fighting). */
        public boolean avoidKnockback = false;
        public double knockbackPenaltyPerLevel = 250.0;

        public double lootingBonusPerLevel = 0.0;
        public double fireAspectBonus = 0.0;
        public double sweepingEdgeBonus = 0.0;

        /** Never auto-swap when the target is a player. */
        public boolean neverSwapAgainstPlayers = false;

        /**
         * Hard overrides: entity pattern -&gt; item pattern. First match wins, and the
         * matching item outranks anything the scorer would otherwise pick.
         */
        public Map<String, String> entityOverrides = new LinkedHashMap<>(Map.of(
                "minecraft:ender_dragon", "minecraft:*_axe"
        ));

        public double overrideBonus = 5000.0;
    }

    /** Right-click helpers. Off by default because they change interaction outcomes. */
    public static class Utility {
        public boolean enabled = false;
        public boolean shearsOnSheep = true;
        public boolean boneMealOnCrops = false;
        public boolean hoeOnTillable = false;
        public boolean axeOnStrippable = false;
        public boolean shovelOnPathable = false;
        public boolean bucketOnFluid = false;

        /** Extra rules: target pattern (block or entity id) -&gt; item pattern. */
        public Map<String, String> interactionOverrides = new LinkedHashMap<>();
    }

    public static class Filters {
        public boolean disableWhileSneaking = false;
        public boolean disableWhileRiding = false;
        public boolean disableInCreative = true;
        public boolean disableWhenScreenOpen = true;
        public boolean disableWhileUsingItem = true;

        public List<String> blockedBlocks = new ArrayList<>();
        public List<String> blockedEntities = new ArrayList<>();
        /** Dimension ids, e.g. "minecraft:the_nether". */
        public List<String> blockedDimensions = new ArrayList<>();

        /** Server addresses (substring match, case-insensitive). Singleplayer is "singleplayer". */
        public List<String> blockedServers = new ArrayList<>();
        public boolean onlyOnAllowedServers = false;
        public List<String> allowedServers = new ArrayList<>();
    }

    public static class Items {
        /** Items ToolSwap will never select. */
        public List<String> blacklist = new ArrayList<>(List.of(
                "minecraft:debug_stick", "minecraft:elytra"
        ));
        /** If true, only items matching whitelist may ever be selected. */
        public boolean whitelistOnly = false;
        public List<String> whitelist = new ArrayList<>();

        /** Flat score nudge per item pattern. Negative to demote, positive to promote. */
        public Map<String, Double> priorityBonus = new LinkedHashMap<>(Map.of(
                "minecraft:netherite_*", 10.0
        ));

        /** Items that must never be risked, regardless of Safety settings. */
        public List<String> neverRisk = new ArrayList<>();
    }

    public static class Safety {
        public DurabilityGuard durabilityGuard = DurabilityGuard.ABSOLUTE;
        public int minDurabilityAbsolute = 5;
        public double minDurabilityPercent = 0.05;

        /** Apply the guard only to enchanted / renamed items. */
        public boolean guardOnlyValuable = false;
        /** If everything is below the guard, allow the best guarded tool anyway. */
        public boolean allowGuardedIfNoAlternative = true;

        /**
         * Conservative mode: forces a minimum interval between swaps, disables mid-break
         * swapping, and optionally disables inventory pulls entirely.
         */
        public boolean antiCheatSafeMode = false;
        public int antiCheatMinSwapIntervalTicks = 2;
        public boolean disableInventorySwapsOnMultiplayer = false;
    }
}
