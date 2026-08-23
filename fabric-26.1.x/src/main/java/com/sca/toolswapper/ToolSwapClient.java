package com.sca.toolswapper;

import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.core.SwapController;
import com.sca.toolswapper.ui.Keybinds;
import com.sca.toolswapper.ui.SwapHud;
import com.sca.toolswapper.ui.ToolSwapCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolSwapClient implements ClientModInitializer {

    public static final String MOD_ID = "toolswapper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Identifier for our HUD element. Must be unique across all mods. */
    public static final Identifier HUD_STATUS =
            Identifier.fromNamespaceAndPath(MOD_ID, "status");

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        Keybinds.register();
        ToolSwapCommand.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Keybinds.tick(client);
            SwapController.get().tick(client);
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                HUD_STATUS,
                SwapHud::render);

        LOGGER.info("[ToolSwap] ready - config at {}", ConfigManager.path());
    }
}
