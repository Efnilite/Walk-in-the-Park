package dev.efnilite.ip.chat;

import org.jetbrains.annotations.NotNull;

/**
 * An enum for all available chat types that a player can select while playing
 */
public enum ChatType {

    LOBBY_ONLY,
    PLAYERS_ONLY,
    PUBLIC;

    @NotNull
    public static ChatType getFromString(@NotNull String string) {
        return switch (string.toLowerCase()) {
            case "global", "public" -> PUBLIC;
            case "players", "players-only", "only-players" -> PLAYERS_ONLY;
            default -> LOBBY_ONLY;
        };
    }
}