package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.style.StyleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Registers stuff.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Registry {

    private static final HashMap<String, Mode> modes = new LinkedHashMap<>();
    private static final HashMap<String, StyleType> styleTypes = new LinkedHashMap<>();

    /**
     * Registers a {@link StyleType}.
     *
     * @param type The type.
     */
    public static void register(@NotNull StyleType type) {
        styleTypes.put(type.getName(), type);
        IP.logging().info("Registered style type %s".formatted(type.getName()));
    }

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        modes.put(mode.getName(), mode);
        IP.logging().info("Registered mode %s".formatted(mode.getName()));
    }

    /**
     * @param style The style name.
     * @return The style type based off of style.
     */
    public static StyleType getTypeFromStyle(@NotNull String style) {
        for (StyleType value : styleTypes.values()) {
            if (value.styles.containsKey(style.toLowerCase())) {
                return value;
            }
        }
        return styleTypes.get("default");
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return modes.get(name);
    }

    /**
     * @param name The style name.
     * @return The {@link StyleType} instance. May be null.
     */
    @Nullable
    public static StyleType getStyleType(@NotNull String name) {
        return styleTypes.get(name);
    }

    /**
     * @return All style types in registration order.
     */
    public static List<StyleType> getStyleTypes() {
        return new ArrayList<>(styleTypes.values());
    }

    /**
     * @return All modes in registration order.
     */
    public static List<Mode> getModes() {
        return new ArrayList<>(modes.values());
    }
}