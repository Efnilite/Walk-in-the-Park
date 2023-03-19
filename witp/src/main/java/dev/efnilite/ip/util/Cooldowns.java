package dev.efnilite.ip.util;

import java.util.*;

/**
 * Class for administering and handling cooldowns.
 */
public class Cooldowns {

    private static final Map<UUID, List<CooldownContainer>> cooldowns = new HashMap<>();

    /**
     * Checks to see whether a specific {@link UUID} can perform an action ({@code key}), with the cooldown for that action ({@code cooldownMs}).
     * If the {@link UUID}'s last execution is inside the cooldown, false will be returned.
     * If it is outside the cooldown range, true will be returned. A new last execution time will be registered as well.
     * Example usage:
     * <br><br>
     * <code>
     * if (passes(uuid, "key", 2000)) {<br>
     * // execute code here<br>
     * }<br>
     * </code>
     *
     * @param uuid       The {@link UUID} to which to register this action to.
     * @param key        The key of this action. You can use this as a way of identifying which action was performed.
     * @param cooldownMs The cooldown for this action.
     * @return true if the provided UUID has passed the cooldown, false if the provided UUID hasn't.
     */
    public static boolean passes(UUID uuid, String key, long cooldownMs) {
        long now = System.currentTimeMillis();

        List<CooldownContainer> containers;
        if (cooldowns.containsKey(uuid)) {

            containers = cooldowns.get(uuid);

            for (CooldownContainer container : containers) {
                if (container.key.equalsIgnoreCase(key)) {

                    // found already existing cooldown
                    long timeSinceLastExecution = now - container.lastExecuted;

                    if (timeSinceLastExecution < cooldownMs) {
                        return false;
                    }
                }
            }

        } else {
            containers = new ArrayList<>();
        }

        containers.add(new CooldownContainer(key, now));
        cooldowns.put(uuid, containers);
        return true;
    }

    private record CooldownContainer(String key, long lastExecuted) {

    }

}
