package dev.efnilite.witp.player.profile;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of {@link Profile}
 */
public class PlayerProfile implements Profile {

    private final Map<String, String> defaults = new HashMap<>();
    private final Map<String, String> settings = new HashMap<>();

    @Override
    public void setSetting(String setting, String value) {
        settings.put(setting, value);
    }

    @Override
    public void setDefault(String setting, String value) {
        defaults.put(setting, value);
    }

    @Override
    public String getDefault(String setting) {
        return defaults.get(setting);
    }

    @Override
    public String getValue(String setting) {
        String value = settings.get(setting);
        String def = defaults.get(setting);
        return value == null ? (def == null ? "" : def) : value;
    }

    @Override
    public Map<String, String> getDefaults() {
        return defaults;
    }

    @Override
    public Map<String, String> getSettings() {
        return settings;
    }
}