package com.sca.toolswapper.mixin;

import com.sca.toolswapper.core.SwapController;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every hook is at HEAD so the swap lands (and its slot packet is on the wire) before the
 * vanilla action packet that follows it. Ordering on a single connection is guaranteed, so
 * the server always attributes the action to the new item.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void toolswap$onAttackBlock(BlockPos pos, Direction direction,
                                        CallbackInfoReturnable<Boolean> cir) {
        SwapController.get().onBlockAttack(pos);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void toolswap$onBreakProgress(BlockPos pos, Direction direction,
                                          CallbackInfoReturnable<Boolean> cir) {
        SwapController.get().onBlockBreakingProgress(pos);
    }

    @Inject(method = "attack", at = @At("HEAD"))
    private void toolswap$onAttackEntity(Player player, Entity target, CallbackInfo ci) {
        SwapController.get().onEntityAttack(target);
    }

    @Inject(method = "interact", at = @At("HEAD"))
    private void toolswap$onInteractEntity(Player player, Entity entity, EntityHitResult hitResult,
                                           InteractionHand hand,
                                           CallbackInfoReturnable<InteractionResult> cir) {
        if (hand == InteractionHand.MAIN_HAND) SwapController.get().onEntityInteract(entity);
    }

    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void toolswap$onInteractBlock(LocalPlayer player, InteractionHand hand,
                                          BlockHitResult hitResult,
                                          CallbackInfoReturnable<InteractionResult> cir) {
        if (hand == InteractionHand.MAIN_HAND) SwapController.get().onBlockInteract(hitResult);
    }
}
