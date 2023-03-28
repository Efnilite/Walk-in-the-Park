package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.style.StyleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Class which features registration for custom modes/addons
 */
public final class Registry {

    private final HashMap<String, Gamemode> modes = new LinkedHashMap<>();
    private final HashMap<String, StyleType> styleTypes = new LinkedHashMap<>();

    /**
     * Registers a style type. This doesn't need materials, so you can pass the materials as null.
     * Example: #registerType(new DefaultStyle(null));
     *
     * @param type The style type.
     */
    public void registerType(@NotNull StyleType type) {
        styleTypes.put(type.getName(), type);
        IP.logging().info("Registered style type %s".formatted(type.getName()));
    }

    /**
     * Registers a gamemode. Registrations are only accepted until the first time a player opens the Gamemode menu.
     *
     * @param mode The instance of the gamemode that's to be registered
     */
    public void register(@NotNull Gamemode mode) {
        modes.put(mode.getName(), mode);
        IP.logging().info("Registered gamemode %s".formatted(mode.getName()));
    }

    public StyleType getTypeFromStyle(@NotNull String style) {
        for (StyleType value : styleTypes.values()) {
            if (value.styles.containsKey(style.toLowerCase())) {
                return value;
            }
        }
        return styleTypes.get("default");
    }

    /**
     * @param name The mode name.
     * @return The {@link Gamemode} instance. May be null.
     */
    @Nullable
    public Gamemode getMode(@NotNull String name) {
        return modes.get(name);
    }

    /**
     * @param name The style name.
     * @return The {@link StyleType} instance. May be null.
     */
    @Nullable
    public StyleType getStyleType(@NotNull String name) {
        return styleTypes.get(name);
    }

    public List<StyleType> getStyleTypes() {
        return new ArrayList<>(styleTypes.values());
    }

    public List<Gamemode> getModes() {
        return new ArrayList<>(modes.values());
    }
}