package dev.efnilite.ip.menu;

import dev.efnilite.ip.config.Config;
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
    SCHEMATICS("settings.parkour_settings.items.schematics", "ip.settings.schematics"),
    SCOREBOARD("settings.parkour_settings.items.scoreboard", "ip.settings.show_scoreboard"),
    FALL_MESSAGE("settings.parkour_settings.items.fall_message", "ip.settings.fall_message"),
    PARTICLES("settings.parkour_settings.items.particles", "ip.settings.particles"),
    SOUND("settings.parkour_settings.items.sound", "ip.settings.sound"),
    SPECIAL_BLOCKS("settings.parkour_settings.items.special_blocks", "ip.settings.special_blocks"),

    LANG("settings.lang", "ip.settings.lang"),
    CHAT("settings.chat", "ip.settings.chat"),

    // lobby
    LOBBY("lobby", "ip.lobby"),
    VISIBILITY("lobby.visibility", "ip.lobby.visibility"),
    PLAYER_MANAGEMENT("lobby.player_management", "ip.lobby.player_management"),

    // other
    JOIN("join", "ip.join"),
    QUIT("quit", "ip.quit"),
    ADMIN("admin", "ip.admin");

    /**
     * The path in config files for this option.
     */
    public final String path;

    /**
     * The permission for this option.
     */
    public final String permission;

    ParkourOption(String path, String permission) {
        this.path = path;
        this.permission = permission;
    }

    /**
     * @param permissible The player
     * @return True if the player is allowed to view/perform this option, false if not.
     */
    public boolean mayPerform(Permissible permissible) {
        boolean value = Option.OPTIONS_ENABLED.getOrDefault(this, true);

        if (value) {
            if (Config.CONFIG.getBoolean("permissions.enabled")) {
                return permissible.hasPermission(permission);
            }
            return true;
        }
        return false;
    }
}