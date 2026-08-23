package com.sca.toolswapper.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Dotted-path reflection over the config objects, so {@code /toolswapper set} can reach every
 * single option without a hand-written command tree per field.
 *
 * <p>Example paths: {@code mining.minSpeedGainRatio}, {@code general.revertMode},
 * {@code safety.antiCheatSafeMode}, {@code items.blacklist}.
 */
public final class ConfigReflect {

    private ConfigReflect() {}

    /** Every settable path under {@code root}, depth-first. */
    public static List<String> paths(Object root) {
        List<String> out = new ArrayList<>();
        collect(root, "", out, 0);
        return out;
    }

    private static void collect(Object obj, String prefix, List<String> out, int depth) {
        if (obj == null || depth > 4) return;
        for (Field f : obj.getClass().getFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            String path = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
            Class<?> t = f.getType();
            if (isLeaf(t)) {
                out.add(path);
            } else {
                try {
                    collect(f.get(obj), path, out, depth + 1);
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    private static boolean isLeaf(Class<?> t) {
        return t.isPrimitive()
                || t == String.class
                || t.isEnum()
                || Number.class.isAssignableFrom(t)
                || t == Boolean.class
                || List.class.isAssignableFrom(t)
                || java.util.Map.class.isAssignableFrom(t);
    }

    public static Object get(Object root, String path) throws ReflectiveOperationException {
        Resolved r = resolve(root, path);
        return r.field.get(r.owner);
    }

    /** Parses {@code value} into the field's type and assigns it. */
    public static void set(Object root, String path, String value) throws ReflectiveOperationException {
        Resolved r = resolve(root, path);
        Class<?> t = r.field.getType();
        Object parsed;

        if (t == boolean.class || t == Boolean.class) {
            parsed = Boolean.parseBoolean(value);
        } else if (t == int.class || t == Integer.class) {
            parsed = Integer.parseInt(value.trim());
        } else if (t == long.class || t == Long.class) {
            parsed = Long.parseLong(value.trim());
        } else if (t == float.class || t == Float.class) {
            parsed = Float.parseFloat(value.trim());
        } else if (t == double.class || t == Double.class) {
            parsed = Double.parseDouble(value.trim());
        } else if (t == String.class) {
            parsed = value;
        } else if (t.isEnum()) {
            parsed = parseEnum(t, value);
        } else if (List.class.isAssignableFrom(t)) {
            parsed = parseList(r.field, value);
        } else {
            throw new IllegalArgumentException("Cannot set " + path + " (type " + t.getSimpleName()
                    + "). Edit toolswapper.json directly for this one.");
        }
        r.field.set(r.owner, parsed);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object parseEnum(Class<?> t, String value) {
        for (Object c : t.getEnumConstants()) {
            if (((Enum) c).name().equalsIgnoreCase(value.trim())) return c;
        }
        throw new IllegalArgumentException("Expected one of "
                + java.util.Arrays.toString(t.getEnumConstants()));
    }

    private static Object parseList(Field field, String value) {
        List<String> parts = new ArrayList<>();
        for (String s : value.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) parts.add(trimmed);
        }
        // Integer lists (allowedHotbarSlots) vs string lists.
        var generic = field.getGenericType().getTypeName();
        if (generic.contains("Integer")) {
            List<Integer> ints = new ArrayList<>();
            for (String s : parts) ints.add(Integer.parseInt(s));
            return ints;
        }
        return parts;
    }

    private static Resolved resolve(Object root, String path) throws ReflectiveOperationException {
        String[] parts = path.split("\\.");
        Object owner = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Field f = owner.getClass().getField(parts[i]);
            owner = f.get(owner);
            if (owner == null) throw new IllegalArgumentException("Unknown path segment: " + parts[i]);
        }
        Field field = owner.getClass().getField(parts[parts.length - 1]);
        return new Resolved(owner, field);
    }

    private record Resolved(Object owner, Field field) {}
}
