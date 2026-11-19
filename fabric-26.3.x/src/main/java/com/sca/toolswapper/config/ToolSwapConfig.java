package com.sca.toolswapper.config;

import com.sca.toolswapper.config.Enums.FeedbackStyle;
import com.sca.toolswapper.config.Enums.HudAnchor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Root config object. Serialised to {@code config/toolswapper.json}. */
public class ToolSwapConfig {

    public int configVersion = 1;

    /** Global kill switch, toggled by the keybind. */
    public boolean masterEnabled = true;

    /** Name of the profile currently in use. */
    public String activeProfile = "default";

    /** Dimension id -&gt; profile name. Applied automatically on dimension change. */
    public Map<String, String> dimensionProfiles = new LinkedHashMap<>();

    /** All defined profiles. Always contains at least "default". */
    public Map<String, Profile> profiles = new LinkedHashMap<>();

    public Display display = new Display();
    public Debug debug = new Debug();

    public ToolSwapConfig() {
        profiles.put("default", new Profile());
    }

    public Profile active() {
        Profile p = profiles.get(activeProfile);
        if (p == null) {
            p = profiles.computeIfAbsent("default", k -> new Profile());
            activeProfile = "default";
        }
        return p;
    }

    public static class Display {
        public boolean hud = true;
        public HudAnchor anchor = HudAnchor.TOP_LEFT;
        public int offsetX = 4;
        public int offsetY = 4;
        public double scale = 1.0;
        public boolean showProfile = true;
        public boolean showLastSwap = true;
        public boolean showScore = false;
        public boolean showOnlyWhenActive = false;
        /** 0xRRGGBB. */
        public int colorEnabled = 0x55FF88;
        public int colorDisabled = 0xFF6666;
        public int colorDetail = 0xBBBBBB;
        public boolean shadow = true;

        public FeedbackStyle feedback = FeedbackStyle.NONE;
        public boolean playSound = false;
        public String soundId = "minecraft:ui.button.click";
        public float soundVolume = 0.25f;
        public float soundPitch = 1.7f;
    }

    public static class Debug {
        /** Log the chosen slot and reason to the game log. */
        public boolean logDecisions = false;
        /** Log the full score table for every candidate. Very spammy. */
        public boolean logScores = false;
    }
}
