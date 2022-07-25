package dev.efnilite.ip.chat;

import org.jetbrains.annotations.NotNull;

/**
 * An enum for all available chat types that a player can select while playing
 */
public enum ChatType {

    LOBBY_ONLY("lobby only"),
    PLAYERS_ONLY("players only"),
    PUBLIC("public");

    public final String name;

    ChatType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @NotNull
    public static ChatType getFromString(@NotNull String string) {
        return switch (string.toLowerCase()) {
            case "global", "public" -> PUBLIC;
            case "players", "players-only", "only-players" -> PLAYERS_ONLY;
            default -> LOBBY_ONLY;
        };
    }
}