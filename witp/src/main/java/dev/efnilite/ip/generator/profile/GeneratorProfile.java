package dev.efnilite.ip.generator.profile;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Profile}
 */
public class GeneratorProfile implements Profile {

    private final Map<String, ProfileValue> settings = new HashMap<>();

    @NotNull
    @Override
    public Profile setSetting(@NotNull String setting, @NotNull String value) {
        settings.put(setting, new ProfileValue(value));
        return this;
    }

    @NotNull
    @Override
    public ProfileValue getValue(@NotNull String setting) {
        ProfileValue value = settings.get(setting);
        return value == null ? new ProfileValue("") : value;
    }

    @Override
    public @NotNull Map<String, ProfileValue> getSettings() {
        return settings;
    }

    @Override
    public void clear() {
        settings.clear();
    }
}