package com.sca.toolswapper.ui;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.sca.toolswapper.config.ConfigManager;
import com.sca.toolswapper.config.ConfigReflect;
import com.sca.toolswapper.config.Profile;
import com.sca.toolswapper.config.ToolSwapConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * {@code /toolswapper ...} - full runtime control without leaving the game.
 *
 * <pre>
 *   /toolswapper on|off
 *   /toolswapper profile list|use &lt;name&gt;|new &lt;name&gt;|copy &lt;name&gt;|delete &lt;name&gt;
 *   /toolswapper set &lt;path&gt; &lt;value&gt;      per-profile option
 *   /toolswapper get &lt;path&gt;
 *   /toolswapper list [filter]              every settable path
 *   /toolswapper global set|get &lt;path&gt; ... display/debug/dimensionProfiles
 *   /toolswapper reload | save
 * </pre>
 */
public final class ToolSwapCommand {

    private ToolSwapCommand() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(literal("toolswapper")

                        .then(literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
                        .then(literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))

                        .then(literal("reload").executes(ctx -> {
                            ConfigManager.reload();
                            return ok(ctx.getSource(), "reloaded from " + ConfigManager.path());
                        }))
                        .then(literal("save").executes(ctx -> {
                            ConfigManager.save();
                            return ok(ctx.getSource(), "saved to " + ConfigManager.path());
                        }))

                        .then(literal("profile")
                                .then(literal("list").executes(ctx -> {
                                    ToolSwapConfig cfg = ConfigManager.get();
                                    return ok(ctx.getSource(), "profiles: "
                                            + String.join(", ", cfg.profiles.keySet())
                                            + "  (active: " + cfg.activeProfile + ")");
                                }))
                                .then(literal("use").then(argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ToolSwapConfig cfg = ConfigManager.get();
                                            if (!cfg.profiles.containsKey(name)) {
                                                return err(ctx.getSource(), "no such profile: " + name);
                                            }
                                            cfg.activeProfile = name;
                                            ConfigManager.save();
                                            return ok(ctx.getSource(), "active profile: " + name);
                                        })))
                                .then(literal("new").then(argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ConfigManager.get().profiles.putIfAbsent(name, new Profile());
                                            ConfigManager.save();
                                            return ok(ctx.getSource(), "created profile " + name);
                                        })))
                                .then(literal("delete").then(argument("name", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ToolSwapConfig cfg = ConfigManager.get();
                                            if ("default".equals(name)) {
                                                return err(ctx.getSource(), "cannot delete the default profile");
                                            }
                                            cfg.profiles.remove(name);
                                            if (name.equals(cfg.activeProfile)) cfg.activeProfile = "default";
                                            ConfigManager.save();
                                            return ok(ctx.getSource(), "deleted profile " + name);
                                        }))))

                        .then(literal("set")
                                .then(argument("path", StringArgumentType.word())
                                        .then(argument("value", StringArgumentType.greedyString())
                                                .executes(ctx -> applySet(ctx.getSource(),
                                                        ConfigManager.profile(),
                                                        StringArgumentType.getString(ctx, "path"),
                                                        StringArgumentType.getString(ctx, "value"))))))

                        .then(literal("get")
                                .then(argument("path", StringArgumentType.word())
                                        .executes(ctx -> applyGet(ctx.getSource(),
                                                ConfigManager.profile(),
                                                StringArgumentType.getString(ctx, "path")))))

                        .then(literal("global")
                                .then(literal("set")
                                        .then(argument("path", StringArgumentType.word())
                                                .then(argument("value", StringArgumentType.greedyString())
                                                        .executes(ctx -> applySet(ctx.getSource(),
                                                                ConfigManager.get(),
                                                                StringArgumentType.getString(ctx, "path"),
                                                                StringArgumentType.getString(ctx, "value"))))))
                                .then(literal("get")
                                        .then(argument("path", StringArgumentType.word())
                                                .executes(ctx -> applyGet(ctx.getSource(),
                                                        ConfigManager.get(),
                                                        StringArgumentType.getString(ctx, "path"))))))

                        .then(literal("list")
                                .executes(ctx -> listPaths(ctx.getSource(), ""))
                                .then(argument("filter", StringArgumentType.greedyString())
                                        .executes(ctx -> listPaths(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "filter")))))
                ));
    }

    private static int setEnabled(FabricClientCommandSource src, boolean value) {
        ConfigManager.get().masterEnabled = value;
        ConfigManager.save();
        return ok(src, value ? "enabled" : "disabled");
    }

    private static int applySet(FabricClientCommandSource src, Object root, String path, String value) {
        try {
            ConfigReflect.set(root, path, value);
            ConfigManager.save();
            return ok(src, path + " = " + value);
        } catch (Exception e) {
            return err(src, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static int applyGet(FabricClientCommandSource src, Object root, String path) {
        try {
            return ok(src, path + " = " + ConfigReflect.get(root, path));
        } catch (Exception e) {
            return err(src, "unknown path: " + path);
        }
    }

    private static int listPaths(FabricClientCommandSource src, String filter) {
        var matches = new ArrayList<String>();
        for (String p : ConfigReflect.paths(ConfigManager.profile())) {
            if (filter.isBlank() || p.toLowerCase().contains(filter.toLowerCase())) matches.add(p);
        }
        if (matches.isEmpty()) return err(src, "no paths match '" + filter + "'");
        return ok(src, matches.size() + " paths:\n" + String.join("\n", matches));
    }

    private static int ok(FabricClientCommandSource src, String message) {
        src.sendFeedback(Component.literal("[ToolSwap] " + message));
        return 1;
    }

    private static int err(FabricClientCommandSource src, String message) {
        src.sendError(Component.literal("[ToolSwap] " + message));
        return 0;
    }
}
