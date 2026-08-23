package com.sca.toolswapper;

import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.core.SwapController;
import com.sca.toolswapper.ui.Keybinds;
import com.sca.toolswapper.ui.SwapHud;
import com.sca.toolswapper.ui.ToolSwapCommand;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToolSwapClient implements ClientModInitializer {

    public static final String MOD_ID = "toolswapper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ConfigManager.load();
        Keybinds.register();
        ToolSwapCommand.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Keybinds.tick(client);
            SwapController.get().tick(client);
        });

        HudRenderCallback.EVENT.register(SwapHud::render);

        LOGGER.info("[ToolSwap] ready - config at {}", ConfigManager.path());
    }
}
