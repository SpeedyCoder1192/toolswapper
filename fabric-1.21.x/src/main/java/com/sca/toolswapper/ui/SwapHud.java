package com.sca.toolswapper.ui;

import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.config.ToolSwapConfig;
import com.sca.toolswapper.core.SwapController;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Minimal status overlay. Everything about it is configurable under {@code display}. */
public final class SwapHud {

    private SwapHud() {}

    public static void render(GuiGraphics context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        ToolSwapConfig cfg = ConfigManager.get();
        ToolSwapConfig.Display d = cfg.display;

        if (!d.hud || client.player == null) return;
        if (client.options.hideGui || client.screen != null) return;

        boolean active = cfg.masterEnabled
                && !SwapController.get().isSuspended()
                && cfg.active().general.enabled;
        if (d.showOnlyWhenActive && !active) return;

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        lines.add(active ? "ToolSwap ON" : "ToolSwap OFF");
        colors.add(active ? d.colorEnabled : d.colorDisabled);

        if (d.showProfile) {
            lines.add("profile: " + cfg.activeProfile);
            colors.add(d.colorDetail);
        }
        if (d.showLastSwap && !SwapController.get().lastItemName().isEmpty()) {
            String text = SwapController.get().lastItemName();
            if (d.showScore) text += " (" + SwapController.get().lastReason() + ")";
            lines.add(text);
            colors.add(d.colorDetail);
        }

        var font = client.font;
        int lineHeight = font.lineHeight + 1;
        int blockHeight = lineHeight * lines.size();
        int widest = 0;
        for (String s : lines) widest = Math.max(widest, font.width(s));

        double scale = Math.max(0.25, d.scale);
        int screenW = (int) (client.getWindow().getGuiScaledWidth() / scale);
        int screenH = (int) (client.getWindow().getGuiScaledHeight() / scale);

        int x = switch (d.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> d.offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - widest - d.offsetX;
        };
        int y = switch (d.anchor) {
            case TOP_LEFT, TOP_RIGHT -> d.offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - blockHeight - d.offsetY;
        };

        context.pose().pushPose();
        context.pose().scale((float) scale, (float) scale, 1.0f);
        for (int i = 0; i < lines.size(); i++) {
            context.drawString(font, lines.get(i), x, y + i * lineHeight,
                    0xFF000000 | colors.get(i), d.shadow);
        }
        context.pose().popPose();
    }
}
