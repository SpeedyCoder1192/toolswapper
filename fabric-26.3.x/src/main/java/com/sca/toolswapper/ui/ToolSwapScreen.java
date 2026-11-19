package com.sca.toolswapper.ui;

import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.config.Profile;
import com.sca.toolswapper.config.ToolSwapConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.Blaze3D;

/**
 * Deliberately small settings screen: the toggles you flip mid-session, plus a shortcut to
 * the JSON where the other few hundred options live. Kept dependency-free so the mod does
 * not need Cloth Config or YACL to be usable.
 */
public class ToolSwapScreen extends Screen {

    private final Screen parent;

    public ToolSwapScreen(Screen parent) {
        super(Component.literal("ToolSwap"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ToolSwapConfig cfg = ConfigManager.get();
        Profile p = cfg.active();

        int w = 200;
        int x = this.width / 2 - w / 2;
        int y = 40;
        int step = 24;

        addRenderableWidget(Button.builder(
                Component.literal("ToolSwap: " + onOff(cfg.masterEnabled)),
                b -> {
                    cfg.masterEnabled = !cfg.masterEnabled;
                    b.setMessage(Component.literal("ToolSwap: " + onOff(cfg.masterEnabled)));
                }).bounds(x, y, w, 20).build());

        y += step;
        addRenderableWidget(Button.builder(
                Component.literal("Mining: " + onOff(p.mining.enabled)),
                b -> {
                    p.mining.enabled = !p.mining.enabled;
                    b.setMessage(Component.literal("Mining: " + onOff(p.mining.enabled)));
                }).bounds(x, y, w, 20).build());

        y += step;
        addRenderableWidget(Button.builder(
                Component.literal("Combat: " + onOff(p.combat.enabled)),
                b -> {
                    p.combat.enabled = !p.combat.enabled;
                    b.setMessage(Component.literal("Combat: " + onOff(p.combat.enabled)));
                }).bounds(x, y, w, 20).build());

        y += step;
        addRenderableWidget(Button.builder(
                Component.literal("Utility: " + onOff(p.utility.enabled)),
                b -> {
                    p.utility.enabled = !p.utility.enabled;
                    b.setMessage(Component.literal("Utility: " + onOff(p.utility.enabled)));
                }).bounds(x, y, w, 20).build());

        y += step;
        addRenderableWidget(Button.builder(
                Component.literal("HUD: " + onOff(cfg.display.hud)),
                b -> {
                    cfg.display.hud = !cfg.display.hud;
                    b.setMessage(Component.literal("HUD: " + onOff(cfg.display.hud)));
                }).bounds(x, y, w, 20).build());

        y += step;
        List<String> names = new ArrayList<>(cfg.profiles.keySet());
        addRenderableWidget(Button.builder(
                Component.literal("Profile: " + cfg.activeProfile),
                b -> {
                    int i = names.indexOf(cfg.activeProfile);
                    cfg.activeProfile = names.get((i + 1) % names.size());
                    b.setMessage(Component.literal("Profile: " + cfg.activeProfile));
                    rebuildWidgets();
                }).bounds(x, y, w, 20).build());

        y += step + 8;
        addRenderableWidget(Button.builder(
                Component.literal("Open toolswapper.json"),
                b -> Blaze3D.openUri(ConfigManager.path().toUri()))
                .bounds(x, y, w, 20).build());

        y += step;
        addRenderableWidget(Button.builder(
                Component.literal("Reload from disk"),
                b -> { ConfigManager.reload(); rebuildWidgets(); })
                .bounds(x, y, w, 20).build());

        y += step;
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(x, y, w, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        super.extractRenderState(ctx, mouseX, mouseY, delta);
        ctx.centeredText(this.font, this.title, this.width / 2, 16, 0xFFFFFF);
        ctx.centeredText(this.font,
                Component.literal("Everything else: /toolswapper set <path> <value>"),
                this.width / 2, this.height - 22, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        if (this.minecraft != null) this.minecraft.gui.setScreen(parent);
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
