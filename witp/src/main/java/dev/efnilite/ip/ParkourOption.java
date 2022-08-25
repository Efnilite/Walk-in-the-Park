package dev.efnilite.ip;

import dev.efnilite.ip.config.Option;
import org.bukkit.entity.Player;

/**
 * An enum for all Parkour Menu Options
 */
public enum ParkourOption {

    // main
    MAIN("main", "ip.main"),

    // play
    PLAY("play", "ip.play"),
    SINGLE("single", "ip.play.single"),
    SPECTATOR("spectator", "ip.play.spectator"),

    // community
    COMMUNITY("community", "ip.community"),
    LEADERBOARDS("leaderboards", "ip.community.leaderboards"),

    // settings
    SETTINGS("settings", "ip.settings"),

    PARKOUR_SETTINGS("parkour_settings", "ip.settings.parkour_settings"),
    STYLES("styles", "ip.settings.styles"),
    LEADS("leads", "ip.settings.leads"),
    TIME("time", "ip.settings.time"),
    SCHEMATICS("schematics", "ip.settings.schematics"),
    USE_SCHEMATICS("use_schematics", "ip.settings.use_schematics"),
    SCHEMATIC_DIFFICULTY("schematic_difficulty", "ip.settings.schematic_difficulty"),
    SHOW_SCOREBOARD("show_scoreboard", "ip.settings.show_scoreboard"),
    SHOW_FALL_MESSAGE("fall_message", "ip.settings.fall_message"),
    PARTICLES_AND_SOUND("particles_and_sound", "ip.settings.particles_and_sound"),
    SPECIAL_BLOCKS("special_blocks", "ip.settings.special_blocks"),
    SCORE_DIFFICULTY("score_difficulty", "ip.settings.score_difficulty"),
    LANG("lang", "ip.settings.lang"),
    CHAT("chat", "ip.settings.chat"),

    // lobby
    LOBBY("lobby", "ip.lobby"),
    VISIBILITY("visibility", "ip.lobby.visibility"),
    PLAYER_MANAGEMENT("player_management", "ip.lobby.player_management"),

    // other
    JOIN("join", "ip.join");

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
        return !Option.PERMISSIONS || player.hasPermission(permission);
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }
}
