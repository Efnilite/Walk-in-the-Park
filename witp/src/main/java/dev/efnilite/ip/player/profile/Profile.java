package dev.efnilite.ip.player.profile;

import java.util.Map;

/**
 * A class for {@link dev.efnilite.ip.player.ParkourUser}'s Profile.
 * This includes settings.
 *
 * @author Efnilite
 */
public interface Profile {

    void setSetting(String setting, String value);

    void setDefault(String setting, String value);

    String getDefault(String setting);

    String getValue(String setting);

    Map<String, String> getDefaults();

    Map<String, String> getSettings();

}