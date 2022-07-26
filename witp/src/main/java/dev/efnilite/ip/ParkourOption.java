package dev.efnilite.ip;

import dev.efnilite.ip.util.config.Option;
import org.bukkit.entity.Player;

/**
 * An enum for all Parkour Menu Options
 */
public enum ParkourOption {

    STYLES("styles", "witp.option.styles"),
    LEADS("leads", "witp.option.leads"),
    TIME("time", "witp.option.time"),
    SCHEMATICS("schematics", "witp.option.schematics"),
    USE_SCHEMATICS("use-schematics", "witp.option.use-schematics"),
    SCHEMATIC_DIFFICULTY("schematic-difficulty", "witp.option.schematic-difficulty"),

    SHOW_SCOREBOARD("show-scoreboard", "witp.option.show-scoreboard"),
    SHOW_FALL_MESSAGE("fall-message", "witp.option.fall-message"),
    PARTICLES_AND_SOUND("particles-and-sound", "witp.option.particles-and-sound"),
    SPECIAL_BLOCKS("special-blocks", "witp.option.special-blocks"),
    SCORE_DIFFICULTY("score-difficulty", "witp.option.score-difficulty"),

    GAMEMODE("gamemode", "witp.option.gamemode"),
    LEADERBOARD("leaderboard", "witp.option.leaderboard"),
    LANGUAGE("language", "witp.option.language"),

    CHAT("chat", "witp.chat"),
    JOIN("join", "witp.join"),
    MENU("menu", "witp.menu"),
    SETTINGS("settings", "witp.option.settings");

    /**
     * The name of the option
     */
    private final String name;

    /**
     * The permission required to change this option
     */
    private final String permission;

    ParkourOption(String name, String permission) {
        this.name = name;
        this.permission = permission;
    }

    /**
     * Checks if a player has the current permission if permissions are enabled.
     * If perms are disabled, always returns true.
     *
     * @param   player
     *          The player
     *
     * @return true if the player is allowed to perform this action, false if not
     */
    public boolean check(Player player) {
        return !Option.PERMISSIONS.get() || player.hasPermission(permission);
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }
}
