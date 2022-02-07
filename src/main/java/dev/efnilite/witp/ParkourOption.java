package dev.efnilite.witp;

/**
 * An enum for all Parkour Menu Options
 */
public enum ParkourOption {

    STYLES("styles", "witp.style"),
    LEAD("lead", "witp.lead");

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
