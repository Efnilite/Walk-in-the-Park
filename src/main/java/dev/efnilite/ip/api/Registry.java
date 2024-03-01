package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.mode.Mode;
import dev.efnilite.ip.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers stuff.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Registry {

    private static final Set<Mode> modes = new HashSet<>();
    private static final Set<Style> styles = new HashSet<>();

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        modes.add(mode);

        IP.logging().info("Registered mode %s".formatted(mode.getName()));
    }

    public static void register(@NotNull Style style) {
        styles.add(style);

        IP.logging().info("Registered style %s".formatted(style.getName()));
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return modes.stream()
                .filter(mode -> mode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static Style getStyle(@NotNull String name) {
        return styles.stream()
                .filter(style -> style.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static Set<Style> getStyles() {
        return styles;
    }

    public static Set<Mode> getModes() {
        return modes;
    }
}