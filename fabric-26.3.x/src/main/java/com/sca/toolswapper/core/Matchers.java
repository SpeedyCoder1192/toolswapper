package com.sca.toolswapper.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pattern matching for every id list in the config.
 *
 * <pre>
 *   minecraft:stone        exact id
 *   *_ore                  wildcard, namespace defaults to minecraft
 *   minecraft:*_ore        wildcard within a namespace
 *   #minecraft:logs        tag reference
 *   #c:ores                tag reference (any namespace)
 *   !minecraft:bedrock     negation - an explicit veto that beats any positive match
 * </pre>
 *
 * Evaluation: if any negated pattern matches, the result is {@code false} immediately.
 * Otherwise the result is {@code true} if any positive pattern matches.
 */
public final class Matchers {

    private static final Map<String, Pattern> REGEX_CACHE = new ConcurrentHashMap<>();

    private Matchers() {}

    public static boolean matchesBlock(BlockState state, List<String> patterns) {
        if (state == null || patterns == null || patterns.isEmpty()) return false;
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return evaluate(patterns, id, tagId -> state.is(TagKey.create(Registries.BLOCK, tagId)));
    }

    public static boolean matchesItem(ItemStack stack, List<String> patterns) {
        if (stack == null || stack.isEmpty() || patterns == null || patterns.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return evaluate(patterns, id, tagId -> stack.is(TagKey.create(Registries.ITEM, tagId)));
    }

    public static boolean matchesEntity(Entity entity, List<String> patterns) {
        if (entity == null || patterns == null || patterns.isEmpty()) return false;
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return evaluate(patterns, id,
                tagId -> entity.is(TagKey.create(Registries.ENTITY_TYPE, tagId)));
    }

    /** Convenience for single-pattern map keys/values. */
    public static boolean matchesBlock(BlockState state, String pattern) {
        return matchesBlock(state, List.of(pattern));
    }

    public static boolean matchesItem(ItemStack stack, String pattern) {
        return matchesItem(stack, List.of(pattern));
    }

    public static boolean matchesEntity(Entity entity, String pattern) {
        return matchesEntity(entity, List.of(pattern));
    }

    /** Simple case-insensitive substring match, used for server address lists. */
    public static boolean matchesText(String value, List<String> patterns) {
        if (value == null || patterns == null || patterns.isEmpty()) return false;
        String lower = value.toLowerCase();
        boolean positive = false;
        for (String raw : patterns) {
            if (raw == null || raw.isBlank()) continue;
            boolean negate = raw.startsWith("!");
            String p = (negate ? raw.substring(1) : raw).toLowerCase();
            boolean hit = p.contains("*") ? regex(p).matcher(lower).matches() : lower.contains(p);
            if (hit) {
                if (negate) return false;
                positive = true;
            }
        }
        return positive;
    }

    private interface TagTest {
        boolean isIn(Identifier tagId);
    }

    private static boolean evaluate(List<String> patterns, Identifier id, TagTest tagTest) {
        if (id == null) return false;
        String full = id.toString();
        boolean positive = false;

        for (String raw : patterns) {
            if (raw == null || raw.isBlank()) continue;
            boolean negate = raw.startsWith("!");
            String p = (negate ? raw.substring(1) : raw).trim();
            if (p.isEmpty()) continue;

            boolean hit;
            if (p.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(withNamespace(p.substring(1)));
                hit = tagId != null && tagTest.isIn(tagId);
            } else if (p.contains("*") || p.contains("?")) {
                hit = regex(withNamespace(p)).matcher(full).matches();
            } else {
                hit = full.equals(withNamespace(p));
            }

            if (hit) {
                if (negate) return false;
                positive = true;
            }
        }
        return positive;
    }

    private static String withNamespace(String p) {
        return p.indexOf(':') >= 0 ? p : "minecraft:" + p;
    }

    private static Pattern regex(String glob) {
        return REGEX_CACHE.computeIfAbsent(glob, g -> {
            StringBuilder sb = new StringBuilder("^");
            for (char c : g.toCharArray()) {
                switch (c) {
                    case '*' -> sb.append(".*");
                    case '?' -> sb.append('.');
                    case '.', '\\', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|' ->
                            sb.append('\\').append(c);
                    default -> sb.append(c);
                }
            }
            return Pattern.compile(sb.append('$').toString());
        });
    }
}
