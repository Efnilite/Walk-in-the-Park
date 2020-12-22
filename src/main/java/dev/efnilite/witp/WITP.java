package dev.efnilite.witp;

import dev.efnilite.witp.command.MainCommand;
import dev.efnilite.witp.generator.ParkourGenerator;
import dev.efnilite.witp.util.Configuration;
import dev.efnilite.witp.util.Metrics;
import dev.efnilite.witp.util.wrapper.BukkitCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class WITP extends JavaPlugin {

    private static WITP instance;
    private static Configuration configuration;

    @Override
    public void onEnable() {
        instance = this;
        configuration = new Configuration(this);

        if (configuration.getFile("config").getBoolean("metrics")) {
            new Metrics(this, 9272);
        }

        ParkourGenerator.Configurable.init();

        addCommand("witp", new MainCommand());
    }

    private void addCommand(String name, BukkitCommand wrapper) {
        PluginCommand command = getCommand(name);

        if (command == null) {
            throw new IllegalStateException("Command is null");
        }

        command.setExecutor(wrapper);
        command.setTabCompleter(wrapper);
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static WITP getInstance() {
        return instance;
    }
}
