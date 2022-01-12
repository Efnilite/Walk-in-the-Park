package dev.efnilite.witp.util.config;

import dev.efnilite.witp.util.Logging;
import org.bukkit.configuration.file.FileConfiguration;

@SuppressWarnings("unchecked")
public class ConfigOption<Type> {

    private Type value;

    public ConfigOption(FileConfiguration config, String path) {
        try {
            value = (Type) config.get(path);
        } catch (ClassCastException ex) {
            Logging.stack("Incompatible types in config option '" + path + "': " + ex.getMessage(),
                    "Please check if you have entered the correct type of data for path '" + path + "'");
        }

        if (value == null) {
            Logging.stack("No value found for option '" + path + "'",
                    "Please check if you have entered anything for path '" + path + "'");
        }
    }

    public ConfigOption(Type value) {
        this.value = value;

        if (value == null) {
            Logging.stack("No value found for unknown option",
                    "Please check if you have entered everything correctly.");
        }
    }

    public void thenSet(Type value) {
        this.value = value;
    }

    public Type get() {
        return value;
    }
}
