package com.sca.toolswapper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.config.ToolSwapConfig;
import com.sca.toolswapper.core.SwapController;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class Keybinds {

    private static final String CATEGORY = "category.toolswapper";

    public static KeyMapping toggle;
    public static KeyMapping cycleProfile;
    public static KeyMapping suspend;
    public static KeyMapping forceSwap;
    public static KeyMapping reload;

    private Keybinds() {}

    public static void register() {
        toggle = bind("key.toolswapper.toggle", GLFW.GLFW_KEY_V);
        cycleProfile = bind("key.toolswapper.cycle_profile", GLFW.GLFW_KEY_UNKNOWN);
        suspend = bind("key.toolswapper.suspend", GLFW.GLFW_KEY_UNKNOWN);
        forceSwap = bind("key.toolswapper.force_swap", GLFW.GLFW_KEY_UNKNOWN);
        reload = bind("key.toolswapper.reload", GLFW.GLFW_KEY_UNKNOWN);
    }

    private static KeyMapping bind(String key, int code) {
        return KeyBindingHelper.registerKeyBinding(
                new KeyMapping(key, InputConstants.Type.KEYSYM, code, CATEGORY));
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

        // "Suspend" is a hold-to-disable key, not a toggle.
        SwapController.get().setSuspended(suspend.isDown());
    }

    private static void say(Minecraft client, String message) {
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal("[ToolSwap] " + message), true);
        }
    }
}
