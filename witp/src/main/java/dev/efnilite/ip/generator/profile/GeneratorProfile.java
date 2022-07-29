package dev.efnilite.ip.generator.profile;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Profile}
 */
public class GeneratorProfile implements Profile {

    private final Map<String, ProfileValue> settings = new HashMap<>();

    @Override
    public Profile setSetting(String setting, String value) {
        settings.put(setting, new ProfileValue(value));
        return this;
    }

    @Override
    public ProfileValue getValue(String setting) {
        return settings.get(setting);
    }

    @Override
    public Map<String, ProfileValue> getSettings() {
        return settings;
    }

    @Override
    public void clear() {
        settings.clear();
    }
}