package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers stuff.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Registry {

    private static final Map<String, Mode> modes = new LinkedHashMap<>();
    private static final Map<String, Style> styles = new LinkedHashMap<>();

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        modes.put(mode.getName(), mode);
        IP.log("Registered mode %s".formatted(mode.getName()));
    }

    public static void register(@NotNull Style style) {
        styles.put(style.name(), style);
        IP.log("Registered style %s".formatted(style.name()));
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return modes.get(name);
    }

    @Nullable
    public static Style getStyle(@NotNull String name) {
        return styles.get(name);
    }

    public static List<Style> getStyles() {
        return new ArrayList<>(styles.values());
    }

    /**
     * @return All modes in registration order.
     */
    public static List<Mode> getModes() {
        return new ArrayList<>(modes.values());
    }
}