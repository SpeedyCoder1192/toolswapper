package com.sca.toolswapper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sca.toolswapper.ToolSwapClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads, saves and hot-reloads {@link ToolSwapConfig}. */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("toolswapper.json");

    private static ToolSwapConfig config = new ToolSwapConfig();

    private ConfigManager() {}

    public static ToolSwapConfig get() {
        return config;
    }

    public static Profile profile() {
        return config.active();
    }

    public static Path path() {
        return PATH;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            config = new ToolSwapConfig();
            save();
            return;
        }
        try {
            String json = Files.readString(PATH);
            ToolSwapConfig loaded = GSON.fromJson(json, ToolSwapConfig.class);
            if (loaded == null) loaded = new ToolSwapConfig();
            if (loaded.profiles == null || loaded.profiles.isEmpty()) {
                loaded.profiles = new ToolSwapConfig().profiles;
            }
            // Repair any nested section that a hand-edited file omitted.
            loaded.profiles.replaceAll((name, p) -> repair(p));
            if (loaded.display == null) loaded.display = new ToolSwapConfig.Display();
            if (loaded.debug == null) loaded.debug = new ToolSwapConfig.Debug();
            config = loaded;
        } catch (Exception e) {
            ToolSwapClient.LOGGER.error("[ToolSwap] Failed to read config, keeping previous values", e);
        }
    }

    private static Profile repair(Profile p) {
        if (p == null) return new Profile();
        if (p.general == null) p.general = new Profile.General();
        if (p.mining == null) p.mining = new Profile.Mining();
        if (p.combat == null) p.combat = new Profile.Combat();
        if (p.utility == null) p.utility = new Profile.Utility();
        if (p.filters == null) p.filters = new Profile.Filters();
        if (p.items == null) p.items = new Profile.Items();
        if (p.safety == null) p.safety = new Profile.Safety();
        return p;
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(config));
        } catch (IOException e) {
            ToolSwapClient.LOGGER.error("[ToolSwap] Failed to write config", e);
        }
    }

    public static void reload() {
        load();
        ToolSwapClient.LOGGER.info("[ToolSwap] Config reloaded from {}", PATH);
    }
}
