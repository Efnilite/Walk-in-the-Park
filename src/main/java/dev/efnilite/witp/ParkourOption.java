package dev.efnilite.witp;

/**
 * An enum for all Parkour Menu Options
 */
public enum ParkourOption {

    STYLES("styles", "witp.option.styles"),
    LEADS("leads", "witp.option.leads"),
    TIME("time", "witp.option.time"),
    DIMENSION("dimension", "witp.option.dimension"),
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
    LANGUAGE("language", "witp.option.language");

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

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }
}
