package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;
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

    private volatile boolean closed;
    private final HashMap<String, Gamemode> gamemodes;
    private final HashMap<String, StyleType> styleTypes;

    public Registry() {
        this.gamemodes = new LinkedHashMap<>();
        this.styleTypes =  new LinkedHashMap<>();
    }

    /**
     * Registers a style type. This doesn't need materials, so you can pass the materials as null.
     * Example: #registerType(new DefaultStyle(null));
     *
     * @param   style
     *          The style type.
     */
    public void registerType(@NotNull StyleType style) {
        if (!closed) {
            this.styleTypes.put(style.getName(), style);
            IP.logging().info("Registered style type " + style.getName() + "!");
        } else {
            throw new IllegalStateException("Register attempt while registry is closed");
        }
    }

    /**
     * Registers a gamemode. Registrations are only accepted until the first time a player opens the Gamemode menu.
     *
     * @param   gamemode
     *          The instance of the gamemode that's to be registered
     */
    public void register(@NotNull Gamemode gamemode) {
        if (!closed) {
            this.gamemodes.put(gamemode.getName(), gamemode);
            IP.logging().info("Registered gamemode " + gamemode.getName() + "!");
        } else {
            throw new IllegalStateException("Register attempt while registry is closed");
        }
    }

    /**
     * Returns a gamemode by a specific name
     *
     * @param   name
     *          The name
     *
     * @return the Gamemode instance associated with this name or null if there is no Gamemode for this name.
     */
    @Nullable
    public Gamemode getGamemode(@NotNull String name) {
        return gamemodes.get(name);
    }

    @Nullable
    public StyleType getStyleType(@NotNull String name) {
        return styleTypes.get(name);
    }

    public StyleType getTypeFromStyle(@NotNull String style) {
        for (StyleType value : styleTypes.values()) {
            if (value.styles.keySet().contains(style.toLowerCase())) {
                return value;
            }
        }
        return styleTypes.get("default");
    }

    public List<StyleType> getStyleTypes() {
        return new ArrayList<>(styleTypes.values());
    }

    public List<Gamemode> getGamemodes() {
        return new ArrayList<>(gamemodes.values());
    }

    public void close() {
        closed = true;
    }
}