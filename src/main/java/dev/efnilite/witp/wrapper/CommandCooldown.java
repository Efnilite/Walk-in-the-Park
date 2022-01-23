package dev.efnilite.witp.wrapper;

/**
 * Class for managing command cooldowns
 */
public class CommandCooldown {

    /**
     * When the command with arg was last executed
     */
    private final long lastExecuted;

    /**
     * The argument
     */
    private final String arg;

    public CommandCooldown(String arg) {
        this.lastExecuted = System.currentTimeMillis();
        this.arg = arg;
    }

    public long getLastExecuted() {
        return lastExecuted;
    }

    public String getArg() {
        return arg;
    }
}
