package com.sca.toolswapper.core;

import com.sca.toolswapper.mixin.ClientPlayerInteractionManagerInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.inventory.ContainerInput;

/** The only place that actually moves items or changes the selected slot. */
public final class SlotSwitcher {

    private SlotSwitcher() {}

    /**
     * Selects a hotbar slot and immediately tells the server.
     *
     * <p>Vanilla syncs the slot from {@code ClientPlayerEntity#tick}, which would mean up to
     * one tick of the server still thinking you hold the old item - long enough for the first
     * block-break packet to be attributed to the wrong tool. We send the packet ourselves and
     * update {@code lastSelectedSlot} through an accessor so vanilla does not send a duplicate.
     */
    public static void select(LocalPlayer player, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot > 8) return;
        if (player.getInventory().getSelectedSlot() == hotbarSlot) return;

        player.getInventory().setSelectedSlot(hotbarSlot);

        Minecraft client = Minecraft.getInstance();
        if (client.gameMode != null) {
            ((ClientPlayerInteractionManagerInvoker) client.gameMode).toolswap$syncSelectedSlot();
        } else {
            player.connection.send(new ServerboundSetCarriedItemPacket(hotbarSlot));
        }
    }

    /**
     * Pulls an item out of the main inventory into a hotbar slot with a SWAP click,
     * then selects it.
     *
     * @param inventorySlot 9-35 (main inventory) or {@link SlotFinder#OFFHAND_SLOT}
     * @param hotbarSlot    0-8 landing pad
     * @return true if a click was actually issued
     */
    public static boolean pullToHotbar(Minecraft client, int inventorySlot, int hotbarSlot) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) return false;
        if (hotbarSlot < 0 || hotbarSlot > 8) return false;

        int screenSlot = toScreenHandlerSlot(inventorySlot);
        if (screenSlot < 0) return false;

        client.gameMode.handleContainerInput(
                player.inventoryMenu.containerId,
                screenSlot,
                hotbarSlot,
                ContainerInput.SWAP,
                player);

        select(player, hotbarSlot);
        return true;
    }

    /**
     * Maps a {@code PlayerInventory} index onto a slot index in the player's own screen handler.
     *
     * <pre>
     *   0        crafting result
     *   1 - 4    crafting grid
     *   5 - 8    armour
     *   9 - 35   main inventory   (inventory index 9 - 35)
     *   36 - 44  hotbar           (inventory index 0 - 8)
     *   45       offhand          (inventory index 40)
     * </pre>
     */
    public static int toScreenHandlerSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) return inventorySlot + 36;
        if (inventorySlot >= 9 && inventorySlot < 36) return inventorySlot;
        if (inventorySlot == SlotFinder.OFFHAND_SLOT) return 45;
        return -1;
    }
}
