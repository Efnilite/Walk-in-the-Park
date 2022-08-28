package dev.efnilite.ip;

import dev.efnilite.ip.config.Option;
import org.bukkit.permissions.Permissible;

/**
 * An enum for all Parkour Menu Options
 */
public enum ParkourOption {

    // main
    MAIN("main", "ip.main"),

    // play
    PLAY("play", "ip.play"),
    SINGLE("play.single", "ip.play.single"),
    SPECTATOR("play.spectator", "ip.play.spectator"),

    // community
    COMMUNITY("community", "ip.community"),
    LEADERBOARDS("community.leaderboards", "ip.community.leaderboards"),

    // settings
    SETTINGS("settings", "ip.settings"),

    PARKOUR_SETTINGS("settings.parkour_settings.item", "ip.settings.parkour_settings"),
    STYLES("settings.parkour_settings.items.styles", "ip.settings.styles"),
    LEADS("settings.parkour_settings.items.leads", "ip.settings.leads"),
    TIME("settings.parkour_settings.items.time", "ip.settings.time"),
    SCHEMATIC_DIFFICULTY("settings.parkour_settings.items.schematic_difficulty", "ip.settings.schematic_difficulty"),
    USE_SCHEMATICS("settings.parkour_settings.items.schematic_use", "ip.settings.schematic_use"),
    SCHEMATIC("settings.parkour_settings.items.schematic", "ip.settings.schematic"),
    SCOREBOARD("settings.parkour_settings.items.scoreboard", "ip.settings.show_scoreboard"),
    FALL_MESSAGE("settings.parkour_settings.items.fall_message", "ip.settings.fall_message"),
    PARTICLES("settings.parkour_settings.items.particles", "ip.settings.particles"),
    SOUND("settings.parkour_settings.items.sound", "ip.settings.sound"),
    SPECIAL_BLOCKS("settings.parkour_settings.items.special_blocks", "ip.settings.special_blocks"),
    SCORE_DIFFICULTY("settings.parkour_settings.items.score_difficulty", "ip.settings.score_difficulty"),

    LANG("settings.lang", "ip.settings.lang"),
    CHAT("settings.chat", "ip.settings.chat"),

    // lobby
    LOBBY("lobby", "ip.lobby"),
    VISIBILITY("lobby.visibility", "ip.lobby.visibility"),
    PLAYER_MANAGEMENT("lobby.player_management", "ip.lobby.player_management"),

    // other
    JOIN("join", "ip.join"),
    ADMIN("admin", "ip.admin");

    /**
     * The name of the option
     */
    private final String path;

    /**
     * The permission required to change this option
     */
    private final String permission;

    ParkourOption(String path, String permission) {
        this.path = path;
        this.permission = permission;
    }

    /**
     * Checks if a permissible has the current permission if permissions are enabled.
     * If perms are disabled, always returns true.
     *
     * @param   permissible
     *          The permissible
     *
     * @return true if the permissible is allowed to perform this action, false if not
     */
    public boolean checkPermission(Permissible permissible) {
        return !Option.PERMISSIONS || permissible.hasPermission(permission);
    }

    /**
     * Checks if a permissible has the current permission, if permissions are enabled.
     * This also checks to see if the option is enabled.
     *
     * @param   permissible
     *          The permissible
     *
     * @return true if the permissible is allowed to view/perform this option, false if not.
     */
    public boolean check(Permissible permissible) {
        return checkPermission(permissible) && (boolean) Option.OPTIONS_DEFAULTS.getOrDefault(this, true);
    }

    public String getPath() {
        return path;
    }

    public String getPermission() {
        return permission;
    }
}
