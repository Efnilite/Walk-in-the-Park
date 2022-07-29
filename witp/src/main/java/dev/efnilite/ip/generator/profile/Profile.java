package dev.efnilite.ip.generator.profile;

import java.util.Map;

/**
 * A class for a {@link dev.efnilite.ip.generator.base.ParkourGenerator}'s Profile.
 *
 * This includes all the settings that this current instance of the generator is using.
 * This class is used to make generator settings modifiable without changing player's
 * preset settings.
 */
public interface Profile {

    Profile setSetting(String setting, String value);

    ProfileValue getValue(String setting);

    Map<String, ProfileValue> getSettings();

    void clear();

}