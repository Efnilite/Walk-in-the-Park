package dev.efnilite.ip.generator.profile;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A class for a {@link dev.efnilite.ip.generator.base.ParkourGenerator}'s Profile.
 * This includes all the settings that this current instance of the generator is using.
 * This class is used to make generator settings modifiable without changing player's
 * preset settings.
 */
public class Profile {

    private final Map<String, ProfileValue> settings = new HashMap<>();

    @NotNull
    public Profile setSetting(@NotNull String setting, @NotNull String value) {
        settings.put(setting, new ProfileValue(value));
        return this;
    }

    @NotNull
    public ProfileValue getValue(@NotNull String setting) {
        ProfileValue value = settings.get(setting);
        return value == null ? new ProfileValue("") : value;
    }

    @NotNull
    public Map<String, ProfileValue> getSettings() {
        return settings;
    }
}