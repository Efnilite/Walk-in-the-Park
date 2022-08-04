package dev.efnilite.ip.generator.profile;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A class for a {@link dev.efnilite.ip.generator.base.ParkourGenerator}'s Profile.
 * This includes all the settings that this current instance of the generator is using.
 * This class is used to make generator settings modifiable without changing player's
 * preset settings.
 */
public interface Profile {

    @NotNull
    Profile setSetting(@NotNull String setting, @NotNull String value);

    @NotNull
    ProfileValue getValue(@NotNull String setting);

    @NotNull Map<String, ProfileValue> getSettings();

    void clear();

}