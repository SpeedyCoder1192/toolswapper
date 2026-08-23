package com.sca.toolswapper.core;

import com.sca.toolswapper.ToolSwapClient;
import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.config.Enums.FeedbackStyle;
import com.sca.toolswapper.config.Enums.RevertMode;
import com.sca.toolswapper.config.Enums.SlotSource;
import com.sca.toolswapper.config.Enums.TriggerMode;
import com.sca.toolswapper.config.Profile;
import com.sca.toolswapper.config.ToolSwapConfig;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The brain. Everything routes through here so that gating, rate limiting, delays and
 * reverts are enforced in exactly one place.
 */
public final class SwapController {

    private static final SwapController INSTANCE = new SwapController();

    public static SwapController get() {
        return INSTANCE;
    }

    private long tick;
    private long lastSwapTick = -1000L;
    private long lastInventorySwapTick = -1000L;
    private int swapsThisSecond;
    private int inventorySwapsThisSecond;
    private long secondWindowStart;

    /** Held-key temporary suspend. */
    private boolean suspended;

    private Runnable pendingSwap;
    private long pendingSwapAtTick;

    private int revertToSlot = -1;
    private int slotAfterOurSwap = -1;
    private long revertAtTick = Long.MAX_VALUE;
    private Object revertTarget;

    private BlockPos lastMinedPos;
    private String lastReason = "";
    private String lastItemName = "";

    private SwapController() {}

    public void setSuspended(boolean value) {
        suspended = value;
        if (value) cancelPending();
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String lastReason() {
        return lastReason;
    }

    public String lastItemName() {
        return lastItemName;
    }

    public void tick(Minecraft client) {
        tick++;
        if (tick - secondWindowStart >= 20) {
            secondWindowStart = tick;
            swapsThisSecond = 0;
            inventorySwapsThisSecond = 0;
        }

        if (pendingSwap != null && tick >= pendingSwapAtTick) {
            Runnable r = pendingSwap;
            pendingSwap = null;
            r.run();
        }

        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        applyDimensionProfile(client);
        handlePreemptive(client, player);
        handleRevert(client, player);
    }

    private void reset() {
        pendingSwap = null;
        revertToSlot = -1;
        revertTarget = null;
        lastMinedPos = null;
    }

    /** Called at the moment left-click starts breaking a block. */
    public void onBlockAttack(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        Profile profile = gate(client);
        if (profile == null || !profile.mining.enabled) return;
        if (profile.mining.trigger == TriggerMode.ON_LOOK) return;

        lastMinedPos = pos;
        evaluateBlock(client, pos, profile);
    }

    /** Called every tick while a block is being broken. */
    public void onBlockBreakingProgress(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        Profile profile = gate(client);
        if (profile == null || !profile.mining.enabled) return;

        boolean newBlock = !pos.equals(lastMinedPos);
        boolean allowed = newBlock
                || (profile.mining.swapMidBreak && !profile.safety.antiCheatSafeMode);
        if (!allowed) return;

        lastMinedPos = pos;
        evaluateBlock(client, pos, profile);
    }

    public void onEntityAttack(Entity target) {
        Minecraft client = Minecraft.getInstance();
        Profile profile = gate(client);
        if (profile == null || !profile.combat.enabled) return;
        if (profile.combat.trigger == TriggerMode.ON_LOOK) return;
        evaluateEntity(client, target, profile);
    }

    public void onEntityInteract(Entity target) {
        Minecraft client = Minecraft.getInstance();
        Profile profile = gate(client);
        if (profile == null || !profile.utility.enabled) return;
        String pattern = resolveUtilityItem(profile, target, null);
        if (pattern != null) applyUtility(client, profile, pattern);
    }

    public void onBlockInteract(BlockHitResult hit) {
        Minecraft client = Minecraft.getInstance();
        Profile profile = gate(client);
        if (profile == null || !profile.utility.enabled || client.level == null) return;
        BlockState state = client.level.getBlockState(hit.getBlockPos());
        String pattern = resolveUtilityItem(profile, null, state);
        if (pattern != null) applyUtility(client, profile, pattern);
    }

    /** Keybind: evaluate whatever the crosshair is on, right now, ignoring trigger mode. */
    public void forceSwapNow() {
        Minecraft client = Minecraft.getInstance();
        Profile profile = gate(client);
        if (profile == null || client.hitResult == null) return;

        if (client.hitResult instanceof BlockHitResult bhr
                && client.hitResult.getType() == HitResult.Type.BLOCK) {
            evaluateBlock(client, bhr.getBlockPos(), profile);
        } else if (client.hitResult instanceof EntityHitResult ehr) {
            evaluateEntity(client, ehr.getEntity(), profile);
        }
    }

    private void handlePreemptive(Minecraft client, LocalPlayer player) {
        Profile profile = gate(client);
        if (profile == null || client.hitResult == null) return;

        HitResult hit = client.hitResult;
        if (hit.getType() == HitResult.Type.BLOCK
                && profile.mining.enabled
                && profile.mining.trigger != TriggerMode.ON_ACTION) {
            evaluateBlock(client, ((BlockHitResult) hit).getBlockPos(), profile);
        } else if (hit.getType() == HitResult.Type.ENTITY
                && profile.combat.enabled
                && profile.combat.trigger != TriggerMode.ON_ACTION) {
            evaluateEntity(client, ((EntityHitResult) hit).getEntity(), profile);
        }
    }

    private void evaluateBlock(Minecraft client, BlockPos pos, Profile profile) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;

        BlockState state = client.level.getBlockState(pos);
        if (state.isAir()) return;
        if (Matchers.matchesBlock(state, profile.filters.blockedBlocks)) return;

        if (profile.mining.skipInstantBreakBlocks
                && MiningMath.breakTicks(player, player.getMainHandItem(), state, pos) <= 1.0) {
            return;
        }

        SlotFinder.Result result = SlotFinder.bestForBlock(player, state, pos, profile);
        if (!result.worthSwapping(profile)) return;

        if (profile.mining.minSpeedGainRatio > 1.0) {
            double held = MiningMath.breakTicks(player, player.getMainHandItem(), state, pos);
            double best = MiningMath.breakTicks(player, result.best().stack(), state, pos);
            if (best > 0 && held / Math.max(best, 0.0001) < profile.mining.minSpeedGainRatio) return;
        }

        debug(profile, result);
        schedule(client, profile, result.best(), state);
    }

    private void evaluateEntity(Minecraft client, Entity target, Profile profile) {
        LocalPlayer player = client.player;
        if (player == null || target == null) return;
        if (Matchers.matchesEntity(target, profile.filters.blockedEntities)) return;
        if (profile.combat.neverSwapAgainstPlayers && target instanceof Player) return;

        if (profile.combat.respectAttackCooldown
                && player.getAttackStrengthScale(0.0f) < profile.combat.minCooldownProgress) {
            return;
        }

        SlotFinder.Result result = SlotFinder.bestForEntity(player, target, profile);
        if (!result.worthSwapping(profile)) return;

        debug(profile, result);
        schedule(client, profile, result.best(), target);
    }

    private void applyUtility(Minecraft client, Profile profile, String itemPattern) {
        LocalPlayer player = client.player;
        if (player == null) return;
        if (Matchers.matchesItem(player.getMainHandItem(), itemPattern)) return;

        Candidate c = SlotFinder.firstMatching(player, profile, itemPattern);
        if (!c.valid()) return;
        schedule(client, profile, c, null);
    }

    private void schedule(Minecraft client, Profile profile, Candidate best, Object target) {
        int delay = Math.max(0, profile.general.swapDelayTicks);
        if (delay == 0) {
            execute(client, profile, best, target);
        } else {
            pendingSwapAtTick = tick + delay;
            pendingSwap = () -> execute(client, profile, best, target);
        }
    }

    private void execute(Minecraft client, Profile profile, Candidate best, Object target) {
        LocalPlayer player = client.player;
        if (player == null || !best.valid()) return;

        Profile.General g = profile.general;
        Profile.Safety safety = profile.safety;

        int cooldown = Math.max(g.swapCooldownTicks,
                safety.antiCheatSafeMode ? safety.antiCheatMinSwapIntervalTicks : 0);
        if (tick - lastSwapTick < cooldown) return;
        if (swapsThisSecond >= Math.max(1, g.maxSwapsPerSecond)) return;

        int originalSlot = player.getInventory().getSelectedSlot();
        boolean ok;

        if (best.isHotbar()) {
            SlotSwitcher.select(player, best.inventorySlot());
            ok = true;
        } else {
            if (!inventoryPullAllowed(client, profile)) return;
            int landing = g.stagingHotbarSlot >= 0 && g.stagingHotbarSlot <= 8
                    ? g.stagingHotbarSlot
                    : originalSlot;
            ok = SlotSwitcher.pullToHotbar(client, best.inventorySlot(), landing);
            if (ok) {
                inventorySwapsThisSecond++;
                lastInventorySwapTick = tick;
            }
        }
        if (!ok) return;

        lastSwapTick = tick;
        swapsThisSecond++;
        slotAfterOurSwap = player.getInventory().getSelectedSlot();
        lastReason = best.reason();
        lastItemName = best.stack().getHoverName().getString();

        armRevert(profile, originalSlot, target);
        feedback(client, profile, best);
    }

    private boolean inventoryPullAllowed(Minecraft client, Profile profile) {
        Profile.General g = profile.general;
        if (g.slotSource == SlotSource.HOTBAR_ONLY) return false;
        if (profile.safety.disableInventorySwapsOnMultiplayer && client.getCurrentServer() != null) {
            return false;
        }
        if (inventorySwapsThisSecond >= Math.max(1, g.maxInventorySwapsPerSecond)) return false;
        return client.screen == null;
    }

    private void armRevert(Profile profile, int originalSlot, Object target) {
        RevertMode mode = profile.general.revertMode;
        if (mode == RevertMode.NEVER) {
            revertToSlot = -1;
            return;
        }
        revertToSlot = originalSlot;
        revertTarget = target;
        revertAtTick = switch (mode) {
            case AFTER_ACTION -> tick + 1;
            case AFTER_DELAY -> tick + Math.max(1, profile.general.revertDelayTicks);
            case ON_TARGET_LOST -> Long.MAX_VALUE;
            case NEVER -> Long.MAX_VALUE;
        };
    }

    private void handleRevert(Minecraft client, LocalPlayer player) {
        if (revertToSlot < 0) return;
        Profile profile = ConfigManager.profile();
        RevertMode mode = profile.general.revertMode;
        if (mode == RevertMode.NEVER) {
            revertToSlot = -1;
            return;
        }

        if (profile.general.revertOnlyIfUntouched
                && player.getInventory().getSelectedSlot() != slotAfterOurSwap) {
            revertToSlot = -1;
            return;
        }

        boolean due = switch (mode) {
            case AFTER_ACTION -> !client.options.keyAttack.isDown() && tick >= revertAtTick;
            case AFTER_DELAY -> tick >= revertAtTick;
            case ON_TARGET_LOST -> targetLost(client);
            case NEVER -> false;
        };
        if (!due) return;

        SlotSwitcher.select(player, revertToSlot);
        revertToSlot = -1;
        revertTarget = null;
    }

    private boolean targetLost(Minecraft client) {
        HitResult hit = client.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.MISS) return true;
        if (revertTarget instanceof Entity e && hit instanceof EntityHitResult ehr) {
            return ehr.getEntity() != e;
        }
        if (revertTarget instanceof BlockState && hit instanceof BlockHitResult bhr && client.level != null) {
            return !client.level.getBlockState(bhr.getBlockPos()).equals(revertTarget);
        }
        return false;
    }

    /** @return the active profile if ToolSwap may act right now, otherwise {@code null} */
    private Profile gate(Minecraft client) {
        ToolSwapConfig cfg = ConfigManager.get();
        if (!cfg.masterEnabled || suspended) return null;

        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) return null;

        Profile p = cfg.active();
        if (!p.general.enabled) return null;

        Profile.Filters f = p.filters;
        if (f.disableInCreative && client.gameMode.getPlayerMode().isCreative()) return null;
        if (f.disableWhileSneaking && player.isShiftKeyDown()) return null;
        if (f.disableWhileRiding && player.isPassenger()) return null;
        if (f.disableWhenScreenOpen && client.screen != null) return null;
        if (f.disableWhileUsingItem && player.isUsingItem()) return null;

        Identifier dim = client.level.dimension().identifier();
        if (Matchers.matchesText(dim.toString(), f.blockedDimensions)) return null;

        String server = serverAddress(client);
        if (Matchers.matchesText(server, f.blockedServers)) return null;
        if (f.onlyOnAllowedServers && !Matchers.matchesText(server, f.allowedServers)) return null;

        return p;
    }

    private static String serverAddress(Minecraft client) {
        var entry = client.getCurrentServer();
        if (entry != null && entry.ip != null) return entry.ip;
        return "singleplayer";
    }

    private void applyDimensionProfile(Minecraft client) {
        ToolSwapConfig cfg = ConfigManager.get();
        if (cfg.dimensionProfiles.isEmpty() || client.level == null) return;
        String dim = client.level.dimension().identifier().toString();
        String wanted = cfg.dimensionProfiles.get(dim);
        if (wanted != null && cfg.profiles.containsKey(wanted) && !wanted.equals(cfg.activeProfile)) {
            cfg.activeProfile = wanted;
        }
    }

    private String resolveUtilityItem(Profile profile, Entity entity, BlockState state) {
        Profile.Utility u = profile.utility;

        for (Map.Entry<String, String> e : u.interactionOverrides.entrySet()) {
            boolean hit = entity != null
                    ? Matchers.matchesEntity(entity, e.getKey())
                    : Matchers.matchesBlock(state, e.getKey());
            if (hit) return e.getValue();
        }

        if (entity != null) {
            if (u.shearsOnSheep && Matchers.matchesEntity(entity, "minecraft:sheep")) {
                return "minecraft:shears";
            }
            return null;
        }
        if (state == null) return null;

        if (u.boneMealOnCrops && Matchers.matchesBlock(state, "#minecraft:crops")) {
            return "minecraft:bone_meal";
        }
        if (u.hoeOnTillable && Matchers.matchesBlock(state,
                java.util.List.of("minecraft:dirt", "minecraft:grass_block", "minecraft:dirt_path"))) {
            return "minecraft:*_hoe";
        }
        if (u.axeOnStrippable && Matchers.matchesBlock(state, "#minecraft:logs")) {
            return "minecraft:*_axe";
        }
        if (u.shovelOnPathable && Matchers.matchesBlock(state, "minecraft:grass_block")) {
            return "minecraft:*_shovel";
        }
        if (u.bucketOnFluid && !state.getFluidState().isEmpty()) {
            return "minecraft:bucket";
        }
        return null;
    }

    private void feedback(Minecraft client, Profile profile, Candidate best) {
        ToolSwapConfig.Display d = ConfigManager.get().display;

        if (d.feedback != FeedbackStyle.NONE && client.player != null) {
            Component msg = Component.literal("[ToolSwap] " + lastItemName + " (" + best.reason() + ")");
            if (d.feedback == FeedbackStyle.ACTION_BAR) {
                client.player.sendOverlayMessage(msg);
            } else {
                client.player.sendSystemMessage(msg);
            }
        }

        if (d.playSound && client.player != null) {
            Identifier id = Identifier.tryParse(d.soundId);
            if (id != null) {
                SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
                if (event != null) {
                    client.player.playSound(event, d.soundVolume, d.soundPitch);
                }
            }
        }
    }

    private void debug(Profile profile, SlotFinder.Result result) {
        var dbg = ConfigManager.get().debug;
        if (dbg.logDecisions) {
            ToolSwapClient.LOGGER.info("[ToolSwap] -> slot {} ({}) reason={}",
                    result.best().inventorySlot(),
                    result.best().stack().getHoverName().getString(),
                    result.best().reason());
        }
        if (dbg.logScores) {
            for (Candidate c : result.all()) {
                ToolSwapClient.LOGGER.info("[ToolSwap]    slot {} {} score={} ({})",
                        c.inventorySlot(), c.stack().getHoverName().getString(), c.score(), c.reason());
            }
        }
    }

    private void cancelPending() {
        pendingSwap = null;
    }
}
