package com.sca.toolswapper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.config.ToolSwapConfig;
import com.sca.toolswapper.core.SwapController;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class Keybinds {

    /**
     * Key categories are registry objects in 26.1, not raw translation keys. The label is
     * derived from the id as {@code key.category.<namespace>.<path>}.
     */
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("toolswapper", "main"));

    public static KeyMapping toggle;
    public static KeyMapping cycleProfile;
    public static KeyMapping suspend;
    public static KeyMapping forceSwap;
    public static KeyMapping reload;

    private Keybinds() {}

    public static void register() {
        toggle = bind("key.toolswapper.toggle", InputConstants.KEY_V);
        cycleProfile = bind("key.toolswapper.cycle_profile", InputConstants.UNKNOWN.getValue());
        suspend = bind("key.toolswapper.suspend", InputConstants.UNKNOWN.getValue());
        forceSwap = bind("key.toolswapper.force_swap", InputConstants.UNKNOWN.getValue());
        reload = bind("key.toolswapper.reload", InputConstants.UNKNOWN.getValue());
    }

    private static KeyMapping bind(String key, int code) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(key, InputConstants.Type.KEYBOARD, code, CATEGORY));
    }

    public static void tick(Minecraft client) {
        ToolSwapConfig cfg = ConfigManager.get();

        while (toggle.consumeClick()) {
            cfg.masterEnabled = !cfg.masterEnabled;
            ConfigManager.save();
            say(client, cfg.masterEnabled ? "ToolSwap enabled" : "ToolSwap disabled");
        }

        while (cycleProfile.consumeClick()) {
            List<String> names = new ArrayList<>(cfg.profiles.keySet());
            if (!names.isEmpty()) {
                int i = names.indexOf(cfg.activeProfile);
                cfg.activeProfile = names.get((i + 1) % names.size());
                ConfigManager.save();
                say(client, "Profile: " + cfg.activeProfile);
            }
        }

        while (forceSwap.consumeClick()) {
            SwapController.get().forceSwapNow();
        }

        while (reload.consumeClick()) {
            ConfigManager.reload();
            say(client, "ToolSwap config reloaded");
        }

        SwapController.get().setSuspended(suspend.isDown());
    }

    private static void say(Minecraft client, String message) {
        if (client.player != null) {
            client.player.sendOverlayMessage(Component.literal("[ToolSwap] " + message));
        }
    }
}
