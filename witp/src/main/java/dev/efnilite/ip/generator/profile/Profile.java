package dev.efnilite.ip.generator.profile;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for setting storing.
 * Allows different modes to change settings without them being saved as being player-selected.
 */
public class Profile {

    /**
     * Map of the setting's name and the value.
     */
    public final Map<String, ProfileValue> settings = new HashMap<>();

    @NotNull
    public Profile set(@NotNull String setting, @NotNull String value) {
        settings.put(setting, new ProfileValue(value));
        return this;
    }

    @NotNull
    public ProfileValue get(@NotNull String setting) {
        ProfileValue value = settings.get(setting);
        return value == null ? new ProfileValue("") : value;
    }


    /**
     * Represents a setting.
     *
     * @param value The string value
     */
    public record ProfileValue(String value) {

        /**
         * Returns true when the string value is "true".
         *
         * @return The value as true or false.
         */
        public boolean asBoolean() {
            return value.equals("true"); // save parsing
        }

        /**
         * Returns the value as a double.
         * @return The value as a double.
         */
        public double asDouble() {
            return Double.parseDouble(value);
        }

        /**
         * Returns the value as an int.
         *
         * @return The value as an int.
         */
        public int asInt() {
            return Integer.parseInt(value);
        }
    }
}